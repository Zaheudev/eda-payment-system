package com.zaheudev.emulator.kafka.producer;

import com.zaheudev.shared.avro.CaptureCompletedEvent;
import com.zaheudev.shared.avro.AuthorizationCompletedEvent;
import com.zaheudev.shared.avro.RefundCompletedEvent;
import com.zaheudev.shared.avro.VoidCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmulatorProcessorProducer {
    private final String AUTHORIZATION_TOPIC_NAME = "authorization-completed";
    private final String CAPTURE_COMPLETED_TOPIC = "capture-completed";
    private final String REFUND_COMPLETED_TOPIC = "refund-completed";
    private final String VOID_COMPLETED_TOPIC = "void-completed";

    private KafkaTemplate<String, Object> kafkaTemplate;

    public EmulatorProcessorProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAuthorizationCompleteEvent(AuthorizationCompletedEvent event) {
        kafkaTemplate.send(AUTHORIZATION_TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing Authorization event: " + e.getMessage());
                    } else {
                        System.out.println("Authorization event published successfully");
                    }
                });
    }

    public void publishCaptureCompletedEvent(CaptureCompletedEvent event) {
        kafkaTemplate.send(CAPTURE_COMPLETED_TOPIC, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing Capture event: " + e.getMessage());
                    } else {
                        System.out.println("Capture event published successfully");
                    }
                });
    }

    public void publishRefundCompletedEvent(RefundCompletedEvent event) {
        kafkaTemplate.send(REFUND_COMPLETED_TOPIC, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing Refund event: " + e.getMessage());
                    } else {
                        System.out.println("Refund event published successfully");
                    }
                });
    }

    public void publishVoidCompletedEvent(VoidCompletedEvent event) {
        kafkaTemplate.send(VOID_COMPLETED_TOPIC, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing Void event: " + e.getMessage());
                    } else {
                        System.out.println("Void event published successfully");
                    }
                });
    }
}
