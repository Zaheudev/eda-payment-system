package com.zaheudev.integration;

import com.zaheudev.integration.metrics.EventRecorder;
import com.zaheudev.integration.metrics.MetricsCollector.ScenarioMetrics;
import com.zaheudev.integration.metrics.ReportWriter;
import com.zaheudev.integration.risk.DeterministicRiskService;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class D_LooseCouplingIT extends BaseIT {

    @AfterAll
    static void tearDown() { stopSystem(); }

    private static final String SCENARIO = "D - Loose Coupling (Emulator Down)";
    private static final List<String> paymentIds = new CopyOnWriteArrayList<>();

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

        log.info("Waiting for emulator to process backlog...");
        sleep(15000);

        long authorized = 0;
        var end = Instant.now();
        for (var pid : paymentIds) {
            try {
                var resp = getJson("/api/v1/payments/" + pid);
                String status = A_HappyPathLifecycleIT.extractField(resp.body(), "paymentStatus");
                if ("AUTHORIZED".equals(status)) authorized++;
                eventRecorder.latencyBetween(pid, "payment-requests", "authorization-completed")
                        .ifPresent(m::addEndToEndLatency);
            } catch (Exception ignored) {}
        }

        m.setTotalDurationMs(end.toEpochMilli() - start.toEpochMilli());
        m.addNote("Authorized: " + authorized + " / " + paymentIds.size());
        m.addNote("Key EDA property: gateway available even when downstream emulator is down");
        m.setStatus("PASSED");

        assertThat(authorized).isEqualTo(paymentIds.size());

        new ReportWriter(metrics).write();
    }

    private void startAllExceptEmulator() {
        services = new ServiceManager();
        services.startAll();
        services.stopEmulator();
        eventRecorder = new EventRecorder();
    }
}
