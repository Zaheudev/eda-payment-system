package com.zaheudev.vaadin.kafka;

import com.zaheudev.vaadin.model.EventEnvelope;
import com.zaheudev.vaadin.service.EventBroadcaster;
import com.zaheudev.vaadin.service.SagaProjectionService;
import com.zaheudev.vaadin.util.AvroToMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Slf4j
@Component
public class UniversalEventConsumer {

    private final EventBroadcaster broadcaster;
    private final SagaProjectionService saga;

    public UniversalEventConsumer(EventBroadcaster broadcaster, SagaProjectionService saga) {
        this.broadcaster = broadcaster;
        this.saga = saga;
    }

    @KafkaListener(topics = {
            "payment-requests", "risk-assessed", "payment-rejected",
            "routing-completed", "authorization-completed",
            "capture-requests", "capture-completed",
            "refund-requests", "refund-completed",
            "void-requests", "void-completed"
    })
    public void onEvent(ConsumerRecord<String, Object> record) {
        Object original = record.value();
        String eventType = extractEventType(original);
        String paymentId = extractPaymentId(original);

        EventEnvelope envelope = EventEnvelope.builder()
                .topic(record.topic())
                .key(record.key())
                .timestamp(record.timestamp())
                .payload(AvroToMap.convert(original))
                .paymentId(paymentId)
                .partition(record.partition())
                .offset(record.offset())
                .build();

        broadcaster.publish(envelope);
        saga.onEvent(paymentId, eventType);
    }

    private String extractEventType(Object value) {
        if (value instanceof GenericRecord gr) {
            return gr.getSchema().getName();
        }
        return value != null ? value.getClass().getSimpleName() : "Unknown";
    }

    private String extractPaymentId(Object value) {
        if (value instanceof GenericRecord gr && gr.hasField("paymentId")) {
            Object pid = gr.get("paymentId");
            if (pid instanceof CharSequence cs) return cs.toString();
            if (pid instanceof ByteBuffer bb) {
                byte[] bytes = new byte[bb.remaining()];
                bb.duplicate().get(bytes);
                return new String(bytes);
            }
        }
        return null;
    }
}
