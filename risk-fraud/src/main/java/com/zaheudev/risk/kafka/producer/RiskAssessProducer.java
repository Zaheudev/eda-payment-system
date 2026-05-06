package com.zaheudev.risk.kafka.producer;

import com.zaheudev.shared.avro.RiskAssessed;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RiskAssessProducer {
    private final String TOPIC_NAME = "risk-assessed";

    private KafkaTemplate<String, RiskAssessed> kafkaTemplate;

    public RiskAssessProducer(KafkaTemplate<String, RiskAssessed> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRiskAssessmentEvent(RiskAssessed event) {
        kafkaTemplate.send(TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing risk assessment event: " + e.getMessage());
                    } else {
                        System.out.println("Risk assessment event published successfully");
                    }
                });
    }
}
