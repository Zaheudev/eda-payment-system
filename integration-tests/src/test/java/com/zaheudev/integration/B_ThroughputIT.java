package com.zaheudev.integration;

import com.zaheudev.integration.metrics.MetricsCollector.ScenarioMetrics;
import com.zaheudev.integration.metrics.ReportWriter;
import com.zaheudev.integration.risk.DeterministicRiskService;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class B_ThroughputIT extends BaseIT {

    @AfterAll
    static void tearDown() { stopSystem(); }

    private static final int PAYMENT_COUNT = 50;
    private static final String SCENARIO = "B - Throughput & Concurrency";
    private static final List<String> paymentIds = new CopyOnWriteArrayList<>();

    @Test
    @Order(1)
    void fireParallelPayments() throws Exception {
        DeterministicRiskService.resetToApprove();
        startSystem();

        var m = metrics.getOrCreate(SCENARIO);
        m.setStatus("RUNNING");
        var start = Instant.now();

        var executor = Executors.newFixedThreadPool(16);
        var futures = new ArrayList<Future<Void>>();

        for (int i = 0; i < PAYMENT_COUNT; i++) {
            int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    var resp = postJson("/api/v1/payments",
                            buildPaymentRequest("THR-" + idx, (100 + idx) + ".00", "USD"));
                    if (resp.statusCode() == 201) {
                        String pid = A_HappyPathLifecycleIT.extractPaymentId(resp.body());
                        paymentIds.add(pid);
                        m.incrementProcessed();
                    } else {
                        m.incrementError();
                    }
                } catch (IOException | InterruptedException e) {
                    m.incrementError();
                }
                return null;
            }));
        }

        for (var f : futures) f.get(30, TimeUnit.SECONDS);
        executor.shutdown();

        long elapsed = Instant.now().toEpochMilli() - start.toEpochMilli();
        long tps = elapsed > 0 ? (m.getProcessedCount() * 1000 / elapsed) : 0;
        m.setThroughputPerSec(tps);
        m.addNote("Submitted " + PAYMENT_COUNT + " payments in " + elapsed + " ms (" + tps + " tx/s)");
        log.info("Throughput: {} payments in {} ms = {} tx/s", m.getProcessedCount(), elapsed, tps);

        log.info("Waiting for all payments to process...");
        sleep(10000);

        long authorized = 0;
        for (var pid : paymentIds) {
            try {
                var resp = getJson("/api/v1/payments/" + pid);
                String status = A_HappyPathLifecycleIT.extractField(resp.body(), "paymentStatus");
                if ("AUTHORIZED".equals(status)) authorized++;
                eventRecorder.latencyBetween(pid, "payment-requests", "authorization-completed")
                        .ifPresent(m::addEndToEndLatency);
            } catch (Exception ignored) {}
        }

        m.addNote("Authorized: " + authorized + " / " + paymentIds.size());
        m.setTotalDurationMs(Instant.now().toEpochMilli() - start.toEpochMilli());
        m.setStatus("PASSED");

        assertThat(authorized).isGreaterThan(0);

        new ReportWriter(metrics).write();
    }
}
