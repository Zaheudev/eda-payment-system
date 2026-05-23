package com.zaheudev.risk.kafka.producer;

import com.zaheudev.shared.avro.PaymentRejectedEvent;
import com.zaheudev.shared.avro.RiskAssessedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RiskFraudProducer {
    private final String RISK_ASSESSED_TOPIC_NAME = "risk-assessed";
    private final String PAYMENT_REJECTED_TOPIC_NAME = "payment-rejected";

    private KafkaTemplate<String, Object> kafkaTemplate;

    public RiskFraudProducer(KafkaTemplate<String, Object> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRiskAssessmentEvent(RiskAssessedEvent event) {
        kafkaTemplate.send(RISK_ASSESSED_TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing risk assessment event: " + e.getMessage());
                    } else {
                        System.out.println("Risk assessment event published successfully");
                    }
                });
    }

    public void publishPaymentRejectedEvent(PaymentRejectedEvent event) {
        kafkaTemplate.send(PAYMENT_REJECTED_TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing payment rejected event: " + e.getMessage());
                    } else {
                        System.out.println("Payment rejected event published successfully");
                    }
                });
    }
}
