package com.zaheudev.gateway.kafka.producer;

import com.zaheudev.shared.avro.CaptureRequestedEvent;
import com.zaheudev.shared.avro.PaymentRequestedEvent;
import com.zaheudev.shared.avro.RefundRequestedEvent;
import com.zaheudev.shared.avro.VoidRequestedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {
    private final String PAYMENT_REQUEST_TOPIC_NAME = "payment-requests";
    private final String CAPTURE_TOPIC_NAME = "capture-requests";
    private final String REFUND_TOPIC_NAME = "refund-requests";
    private final String VOID_TOPIC_NAME = "void-requests";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentRequestedEvent(PaymentRequestedEvent event) {
        kafkaTemplate.send(PAYMENT_REQUEST_TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.err.println("Failed to publish event: " + e.getMessage());
                    } else {
                        System.out.println("Event published successfully to topic " + PAYMENT_REQUEST_TOPIC_NAME);
                    }
                });
    }

    public void publishCaptureRequestedEvent(CaptureRequestedEvent event) {
        kafkaTemplate.send(CAPTURE_TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.err.println("Failed to publish capture event: " + e.getMessage());
                    } else {
                        System.out.println("Capture event published successfully to topic " + CAPTURE_TOPIC_NAME);
                    }
                });
    }

    public void publishRefundRequestedEvent(RefundRequestedEvent event) {
        kafkaTemplate.send(REFUND_TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.err.println("Failed to publish refund event: " + e.getMessage());
                    } else {
                        System.out.println("Refund event published successfully to topic " + REFUND_TOPIC_NAME);
                    }
                });
    }

    public void publishVoidRequestedEvent(VoidRequestedEvent event) {
        kafkaTemplate.send(VOID_TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.err.println("Failed to publish void event: " + e.getMessage());
                    } else {
                        System.out.println("Void event published successfully to topic " + VOID_TOPIC_NAME);
                    }
                });
    }
}
