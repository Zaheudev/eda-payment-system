package com.zaheudev.integration;

import com.zaheudev.integration.metrics.EventRecorder.RecordedEvent;
import com.zaheudev.integration.metrics.MetricsCollector.ScenarioMetrics;
import com.zaheudev.integration.metrics.ReportWriter;
import com.zaheudev.integration.risk.DeterministicRiskService;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class E_RejectionPathIT extends BaseIT {

    @AfterAll
    static void tearDown() { stopSystem(); }

    private static final String SCENARIO = "E - Rejection Path (CRITICAL risk)";

    @Test
    @Order(1)
    void paymentRejectedOnCriticalRisk() throws Exception {
        DeterministicRiskService.setRejectNext(true);
        startSystem();

        var m = metrics.getOrCreate(SCENARIO);
        m.setStatus("RUNNING");
        var start = Instant.now();

        var resp = postJson("/api/v1/payments", buildPaymentRequest("REJ-001", "999.00", "USD"));
        assertThat(resp.statusCode()).isEqualTo(201);
        String pid = A_HappyPathLifecycleIT.extractPaymentId(resp.body());
        m.incrementProcessed();
        log.info("Rejection-test payment {} submitted", pid);

        await("payment " + pid + " rejected")
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    var finalResp = getJson("/api/v1/payments/" + pid);
                    String status = A_HappyPathLifecycleIT.extractField(finalResp.body(), "paymentStatus");
                    assertThat(status).isEqualTo("REJECTED");
                });

        List<RecordedEvent> paymentEvents = eventRecorder.getEventsForPayment(pid);
        long rejectedForPid = paymentEvents.stream()
                .filter(e -> "payment-rejected".equals(e.topic())).count();
        long authForPid = paymentEvents.stream()
                .filter(e -> "authorization-completed".equals(e.topic())).count();
        long routingForPid = paymentEvents.stream()
                .filter(e -> "routing-completed".equals(e.topic())).count();

        log.info("Rejection path status for {}: REJECTED. payment-rejected={}, auth-completed={}, routing-completed={}",
                pid, rejectedForPid, authForPid, routingForPid);

        m.addNote("Payment status: REJECTED");
        m.addNote("payment-rejected events: " + rejectedForPid);
        m.addNote("authorization-completed events: " + authForPid + " (should be 0)");
        m.addNote("routing-completed events: " + routingForPid + " (should be 0)");
        m.addNote("Key EDA property: rejected payments skip routing and authorization entirely");
        m.setTotalDurationMs(Instant.now().toEpochMilli() - start.toEpochMilli());
        m.setStatus("PASSED");

        assertThat(rejectedForPid).isGreaterThan(0);
        assertThat(authForPid).isEqualTo(0);
        assertThat(routingForPid).isEqualTo(0);

        DeterministicRiskService.resetToApprove();

        new ReportWriter(metrics).write();
    }
}
