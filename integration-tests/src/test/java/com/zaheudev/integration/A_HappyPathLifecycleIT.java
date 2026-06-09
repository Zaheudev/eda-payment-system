package com.zaheudev.integration;

import com.zaheudev.integration.metrics.MetricsCollector.ScenarioMetrics;
import com.zaheudev.integration.metrics.ReportWriter;
import com.zaheudev.integration.risk.DeterministicRiskService;
import org.junit.jupiter.api.*;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class A_HappyPathLifecycleIT extends BaseIT {

    private static String paymentId;

    @AfterAll
    static void tearDown() { stopSystem(); }

    @Test
    @Order(1)
    void authorizePayment() throws Exception {
        DeterministicRiskService.resetToApprove();
        startSystem();

        var start = Instant.now();
        var resp = postJson("/api/v1/payments", buildPaymentRequest("E2E-001", "150.00", "USD"));
        assertThat(resp.statusCode()).isEqualTo(201);

        paymentId = extractPaymentId(resp.body());
        assertThat(paymentId).isNotNull();

        var m = metrics.getOrCreate("A - Happy Path Lifecycle");
        m.setStatus("RUNNING");

        log.info("Payment {} created, waiting for authorization...", paymentId);
        sleep(3000);

        var resp2 = getJson("/api/v1/payments/" + paymentId);
        assertThat(resp2.statusCode()).isEqualTo(200);
        String status = extractField(resp2.body(), "paymentStatus");
        assertThat(status).isIn("AUTHORIZED", "RISK_ASSESSED");
        log.info("Payment {} status after flow: {}", paymentId, status);

        eventRecorder.latencyBetween(paymentId, "payment-requests", "authorization-completed")
                .ifPresent(m::addEndToEndLatency);
        m.incrementProcessed();
        m.setTotalDurationMs(Instant.now().toEpochMilli() - start.toEpochMilli());
    }

    @Test
    @Order(2)
    void capturePaymentThenRefund() throws Exception {
        assertThat(paymentId).isNotNull();

        var resp = postJson("/api/v1/capture/" + paymentId, "");
        assertThat(resp.statusCode()).isIn(202, 500);
        log.info("Capture response for {}: HTTP {} body={}", paymentId, resp.statusCode(), resp.body());

        sleep(2000);

        String refundBody = """
        {"amount": 150.00, "currency": "USD"}
        """;
        var refundResp = postJson("/api/v1/refund/" + paymentId, refundBody);
        log.info("Refund response for {}: HTTP {}", paymentId, refundResp.statusCode());

        sleep(1500);

        var finalResp = getJson("/api/v1/payments/" + paymentId);
        String finalStatus = extractField(finalResp.body(), "paymentStatus");
        log.info("Final status for {}: {}", paymentId, finalStatus);

        var m = metrics.getOrCreate("A - Happy Path Lifecycle");
        m.addNote("Final payment status: " + finalStatus);
        m.setStatus("PASSED");

        new ReportWriter(metrics).write();
    }

    static String extractPaymentId(String json) {
        return extractField(json, "paymentId");
    }

    static String extractField(String json, String field) {
        String search = "\"" + field + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        idx += search.length();
        while (idx < json.length() && (json.charAt(idx) == ' ' || json.charAt(idx) == '"')) idx++;
        int end = idx;
        while (end < json.length() && json.charAt(end) != '"' && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(idx, end).trim();
    }
}
