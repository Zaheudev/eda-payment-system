package com.zaheudev.integration;

import com.zaheudev.integration.metrics.MetricsCollector.ScenarioMetrics;
import com.zaheudev.integration.metrics.ReportWriter;
import com.zaheudev.integration.risk.DeterministicRiskService;
import org.junit.jupiter.api.*;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

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

        sleep(4000);

        var finalResp = getJson("/api/v1/payments/" + pid);
        String status = A_HappyPathLifecycleIT.extractField(finalResp.body(), "paymentStatus");
        log.info("Rejection path status for {}: {}", pid, status);

        long rejectedEvents = eventRecorder.countEventsOnTopic("payment-rejected");
        long authEvents = eventRecorder.countEventsOnTopic("authorization-completed");
        long routingEvents = eventRecorder.countEventsOnTopic("routing-completed");

        m.addNote("Payment status: " + status);
        m.addNote("payment-rejected events: " + rejectedEvents);
        m.addNote("authorization-completed events: " + authEvents + " (should be 0)");
        m.addNote("routing-completed events: " + routingEvents + " (should be 0)");
        m.addNote("Key EDA property: rejected payments skip routing and authorization entirely");
        m.setTotalDurationMs(Instant.now().toEpochMilli() - start.toEpochMilli());
        m.setStatus("PASSED");

        assertThat(status).isEqualTo("REJECTED");
        assertThat(rejectedEvents).isGreaterThan(0);
        assertThat(authEvents).isEqualTo(0);

        DeterministicRiskService.resetToApprove();

        new ReportWriter(metrics).write();
    }
}
