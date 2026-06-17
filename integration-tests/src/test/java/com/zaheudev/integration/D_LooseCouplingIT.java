package com.zaheudev.integration;

import com.zaheudev.integration.metrics.EventRecorder;
import com.zaheudev.integration.metrics.MetricsCollector.ScenarioMetrics;
import com.zaheudev.integration.metrics.ReportWriter;
import com.zaheudev.integration.risk.DeterministicRiskService;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class D_LooseCouplingIT extends BaseIT {

    @AfterAll
    static void tearDown() { stopSystem(); }

    private static final String SCENARIO = "D - Loose Coupling (Emulator Down)";
    private final List<String> paymentIds = new ArrayList<>();

    @Test
    @Order(1)
    void paymentsAcceptedWhileEmulatorDown() throws Exception {
        DeterministicRiskService.resetToApprove();

        startAllExceptEmulator();

        var m = metrics.getOrCreate(SCENARIO);
        m.setStatus("RUNNING");
        var start = Instant.now();

        for (int i = 0; i < 5; i++) {
            var resp = postJson("/api/v1/payments", buildPaymentRequest("LC-" + i, (300 + i) + ".00", "USD"));
            assertThat(resp.statusCode()).isEqualTo(201);
            paymentIds.add(A_HappyPathLifecycleIT.extractPaymentId(resp.body()));
            m.incrementProcessed();
        }
        m.addNote("5 payments submitted while emulator is offline; gateway still returns 201");

        sleep(2000);

        services.startEmulator();
        assertThat(services.isEmulatorAlive()).isTrue();
        m.addNote("Emulator started; backlog will drain from Kafka");

        log.info("Waiting for emulator to process backlog ({} payments)...", paymentIds.size());
        await("all payments AUTHORIZED after emulator recovery")
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    long authorized = 0;
                    for (var pid : paymentIds) {
                        try {
                            var resp = getJson("/api/v1/payments/" + pid);
                            String status = A_HappyPathLifecycleIT.extractField(resp.body(), "paymentStatus");
                            if ("AUTHORIZED".equals(status)) authorized++;
                        } catch (Exception ignored) {}
                    }
                    assertThat(authorized).isEqualTo(paymentIds.size());
                });

        var end = Instant.now();
        for (var pid : paymentIds) {
            eventRecorder.latencyBetween(pid, "payment-requests", "authorization-completed")
                    .ifPresent(m::addEndToEndLatency);
        }

        m.setTotalDurationMs(end.toEpochMilli() - start.toEpochMilli());
        m.addNote("Authorized: " + paymentIds.size() + " / " + paymentIds.size());
        m.addNote("Key EDA property: gateway available even when downstream emulator is down");
        m.setStatus("PASSED");

        new ReportWriter(metrics).write();
    }

    private void startAllExceptEmulator() {
        services = new ServiceManager();
        services.startAll();
        services.stopEmulator();
        eventRecorder = new EventRecorder();
    }
}
