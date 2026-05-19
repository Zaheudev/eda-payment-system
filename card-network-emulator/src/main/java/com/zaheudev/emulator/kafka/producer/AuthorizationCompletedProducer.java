package com.zaheudev.emulator.kafka.producer;

import com.zaheudev.shared.avro.AuthorizationCompleted;
import com.zaheudev.shared.avro.RiskAssessed;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationCompletedProducer {
    private final String TOPIC_NAME = "authorization-completed";

    private KafkaTemplate<String, AuthorizationCompleted> kafkaTemplate;

    public AuthorizationCompletedProducer(KafkaTemplate<String, AuthorizationCompleted> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAuthorizationCompleteEvent(AuthorizationCompleted event) {
        kafkaTemplate.send(TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing Authorization event: " + e.getMessage());
                    } else {
                        System.out.println("Authorization event published successfully");
                    }
                });
    }
}
