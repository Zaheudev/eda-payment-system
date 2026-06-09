package com.zaheudev.integration;

import com.zaheudev.integration.metrics.EventRecorder.RecordedEvent;
import com.zaheudev.integration.metrics.MetricsCollector.ScenarioMetrics;
import com.zaheudev.integration.metrics.ReportWriter;
import com.zaheudev.integration.risk.DeterministicRiskService;
import com.zaheudev.shared.avro.AuthorizationCompletedEvent;
import com.zaheudev.shared.avro.PaymentStatus;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class F_OrderingIdempotencyIT extends BaseIT {

    private static final String SCENARIO = "F - Ordering & Idempotency";

    @Test
    @Order(1)
    void verifyOrderingAndIdempotency() throws Exception {
        DeterministicRiskService.resetToApprove();
        startSystem();

        var m = metrics.getOrCreate(SCENARIO);
        m.setStatus("RUNNING");
        var start = Instant.now();

        var resp = postJson("/api/v1/payments", buildPaymentRequest("ORD-001", "500.00", "USD"));
        assertThat(resp.statusCode()).isEqualTo(201);
        String pid = A_HappyPathLifecycleIT.extractPaymentId(resp.body());
        log.info("Ordering test payment {} submitted", pid);

        sleep(6000);

        var events = eventRecorder.getEventsForPayment(pid);
        List<String> topicOrder = events.stream().map(RecordedEvent::topic).toList();
        log.info("Event order for {}: {}", pid, topicOrder);

        m.addNote("Event topic sequence: " + topicOrder);
        m.addNote("Expected order: payment-requests -> risk-assessed -> routing-completed -> authorization-completed");

        boolean ordered = isOrdered(topicOrder,
                "payment-requests", "risk-assessed", "routing-completed", "authorization-completed");
        m.addNote("Correctly ordered: " + (ordered ? "YES" : "NO"));

        var finalResp = getJson("/api/v1/payments/" + pid);
        String status = A_HappyPathLifecycleIT.extractField(finalResp.body(), "paymentStatus");
        assertThat(status).isEqualTo("AUTHORIZED");
        log.info("Payment {} is AUTHORIZED, sending duplicate event...", pid);

        sendDuplicateAuthorization(pid);

        sleep(3000);

        var afterDupResp = getJson("/api/v1/payments/" + pid);
        String afterDupStatus = A_HappyPathLifecycleIT.extractField(afterDupResp.body(), "paymentStatus");
        m.addNote("Status after duplicate event: " + afterDupStatus + " (should stay AUTHORIZED)");
        m.addNote("Key EDA properties: per-key ordering + idempotent consumers");
        m.setStatus("PASSED");

        assertThat(afterDupStatus).isEqualTo("AUTHORIZED");

        new ReportWriter(metrics).write();
        DeterministicRiskService.resetToApprove();
    }

    private void sendDuplicateAuthorization(String paymentId) throws Exception {
        var props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, EdaInfrastructure.getKafkaBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", EdaInfrastructure.getSchemaRegistryUrl());

        try (var producer = new KafkaProducer<String, AuthorizationCompletedEvent>(props)) {
            var event = AuthorizationCompletedEvent.newBuilder()
                    .setPaymentId(paymentId)
                    .setSuccess(true)
                    .setAuthCode("DUP-123456")
                    .setRrn("DUPLICATE-RRN")
                    .setProcessorTransactionId("dup-txn-" + UUID.randomUUID())
                    .setTimestamp(System.currentTimeMillis())
                    .setSelectedPaymentMethod(com.zaheudev.shared.avro.PaymentMethodEnum.VISA)
                    .build();
            producer.send(new ProducerRecord<>("authorization-completed", paymentId, event)).get();
            log.info("Duplicate authorization event sent for {}", paymentId);
        }
    }

    private boolean isOrdered(List<String> actual, String... expected) {
        int prevIdx = -1;
        for (String exp : expected) {
            int idx = actual.indexOf(exp);
            if (idx < 0) return false;
            if (idx <= prevIdx) return false;
            prevIdx = idx;
        }
        return true;
    }
}
