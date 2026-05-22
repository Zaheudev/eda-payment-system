package com.zaheudev.emulator.kafka.producer;

import com.zaheudev.shared.avro.CaptureCompletedEvent;
import com.zaheudev.shared.avro.AuthorizationCompletedEvent;
import com.zaheudev.shared.avro.RefundCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmulatorProcessorProducer {
    private final String AUTHORIZATION_TOPIC_NAME = "authorization-completed";
    private final String CAPTURE_COMPLETED_TOPIC = "capture-completed";
    private final String REFUND_COMPLETED_TOPIC = "refund-completed";

    private KafkaTemplate<String, AuthorizationCompletedEvent> authorizationKafkaTemplate;
    private KafkaTemplate<String, CaptureCompletedEvent> kafkaTemplateCaptureCompleted;
    private KafkaTemplate<String, RefundCompletedEvent> kafkaTemplateRefundCompleted;

    public EmulatorProcessorProducer(KafkaTemplate<String, AuthorizationCompletedEvent> authorizationKafkaTemplate,
                                     KafkaTemplate<String, CaptureCompletedEvent> kafkaTemplateCaptureCompleted,
                                     KafkaTemplate<String, RefundCompletedEvent> kafkaTemplateRefundCompleted) {
        this.authorizationKafkaTemplate = authorizationKafkaTemplate;
        this.kafkaTemplateCaptureCompleted = kafkaTemplateCaptureCompleted;
        this.kafkaTemplateRefundCompleted = kafkaTemplateRefundCompleted;
    }

    public void publishAuthorizationCompleteEvent(AuthorizationCompletedEvent event) {
        authorizationKafkaTemplate.send(AUTHORIZATION_TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing Authorization event: " + e.getMessage());
                    } else {
                        System.out.println("Authorization event published successfully");
                    }
                });
    }

    public void publishCaptureCompletedEvent(CaptureCompletedEvent event) {
        kafkaTemplateCaptureCompleted.send(CAPTURE_COMPLETED_TOPIC, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing Capture event: " + e.getMessage());
                    } else {
                        System.out.println("Capture event published successfully");
                    }
                });
    }

    public void publishRefundCompletedEvent(RefundCompletedEvent event) {
        kafkaTemplateRefundCompleted.send(REFUND_COMPLETED_TOPIC, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing Refund event: " + e.getMessage());
                    } else {
                        System.out.println("Refund event published successfully");
                    }
                });
    }
}
