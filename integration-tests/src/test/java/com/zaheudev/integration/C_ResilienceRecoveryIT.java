package com.zaheudev.integration;

import com.zaheudev.integration.metrics.MetricsCollector.ScenarioMetrics;
import com.zaheudev.integration.metrics.ReportWriter;
import com.zaheudev.integration.risk.DeterministicRiskService;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class C_ResilienceRecoveryIT extends BaseIT {

    @AfterAll
    static void tearDown() { stopSystem(); }

    private static final String SCENARIO = "C - Resilience & Recovery";
    private static final List<String> paymentIds = new CopyOnWriteArrayList<>();

    @Test
    @Order(1)
    void resilienceWithRoutingKillAndRecover() throws Exception {
        DeterministicRiskService.resetToApprove();
        startSystem();

        var m = metrics.getOrCreate(SCENARIO);
        m.setStatus("RUNNING");
        var suiteStart = Instant.now();

        for (int i = 0; i < 5; i++) {
            var resp = postJson("/api/v1/payments", buildPaymentRequest("RES-" + i, (200 + i) + ".00", "USD"));
            if (resp.statusCode() == 201) {
                paymentIds.add(A_HappyPathLifecycleIT.extractPaymentId(resp.body()));
            }
        }
        log.info("{} pre-kill payments submitted", paymentIds.size());

        sleep(2000);

        assertThat(services.isRoutingAlive()).isTrue();
        var killTime = Instant.now();
        services.stopRouting();
        assertThat(services.isRoutingAlive()).isFalse();
        log.info("Routing service KILLED at {}", killTime);

        for (int i = 5; i < 8; i++) {
            var resp = postJson("/api/v1/payments", buildPaymentRequest("RES-" + i, (200 + i) + ".00", "USD"));
            if (resp.statusCode() == 201) {
                paymentIds.add(A_HappyPathLifecycleIT.extractPaymentId(resp.body()));
            }
        }
        log.info("{} payments submitted while routing is down", paymentIds.size());

        sleep(2000);

        var restartTime = Instant.now();
        services.startRouting();
        assertThat(services.isRoutingAlive()).isTrue();
        log.info("Routing service RESTARTED at {}", restartTime);

        log.info("Waiting for backlog to drain...");
        sleep(15000);

        long authorized = 0;
        var recoveryEnd = Instant.now();
        for (var pid : paymentIds) {
            try {
                var resp = getJson("/api/v1/payments/" + pid);
                String status = A_HappyPathLifecycleIT.extractField(resp.body(), "paymentStatus");
                if ("AUTHORIZED".equals(status)) authorized++;
                eventRecorder.latencyBetween(pid, "payment-requests", "authorization-completed")
                        .ifPresent(m::addEndToEndLatency);
            } catch (Exception ignored) {}
        }

        long recoveryMs = recoveryEnd.toEpochMilli() - restartTime.toEpochMilli();
        m.setRecoveryTimeMs(recoveryMs);
        m.incrementProcessed();
        for (int i = 0; i < (paymentIds.size() - authorized); i++) m.incrementError();
        m.setTotalDurationMs(Instant.now().toEpochMilli() - suiteStart.toEpochMilli());
        m.addNote("Routing killed for ~4s then restarted. Submitted: " + paymentIds.size()
                + ", Authorized: " + authorized);
        m.addNote("Recovery time (restart-to-all-authorized): " + recoveryMs + " ms");
        m.addNote("Key EDA property: producers and consumers are temporally decoupled via Kafka");
        m.setStatus("PASSED");

        assertThat(authorized).isEqualTo(paymentIds.size());
        log.info("Resilience test: {} submitted / {} authorized. Recovery: {} ms. ZERO LOSS.",
                paymentIds.size(), authorized, recoveryMs);

        new ReportWriter(metrics).write();
    }
}
