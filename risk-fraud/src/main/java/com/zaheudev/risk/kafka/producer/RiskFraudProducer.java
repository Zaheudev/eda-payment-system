package com.zaheudev.risk.kafka.producer;

import com.zaheudev.shared.avro.PaymentRejectedEvent;
import com.zaheudev.shared.avro.RiskAssessedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RiskFraudProducer {
    private final String RISK_ASSESSED_TOPIC_NAME = "risk-assessed";
    private final String PAYMENT_REJECTED_TOPIC_NAME = "payment-rejected";

    private KafkaTemplate<String, RiskAssessedEvent> riskAssessedKafkaTemplate;

    private KafkaTemplate<String, PaymentRejectedEvent> paymentRejectedKafkaTemplate;

    public RiskFraudProducer(KafkaTemplate<String, RiskAssessedEvent> riskAssessedKafkaTemplate,
                             KafkaTemplate<String, PaymentRejectedEvent> paymentRejectedKafkaTemplate){
        this.riskAssessedKafkaTemplate = riskAssessedKafkaTemplate;
        this.paymentRejectedKafkaTemplate = paymentRejectedKafkaTemplate;

    }

    public void publishRiskAssessmentEvent(RiskAssessedEvent event) {
        riskAssessedKafkaTemplate.send(RISK_ASSESSED_TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing risk assessment event: " + e.getMessage());
                    } else {
                        System.out.println("Risk assessment event published successfully");
                    }
                });
    }

    public void publishPaymentRejectedEvent(PaymentRejectedEvent event) {
        paymentRejectedKafkaTemplate.send(PAYMENT_REJECTED_TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing payment rejected event: " + e.getMessage());
                    } else {
                        System.out.println("Payment rejected event published successfully");
                    }
                });
    }
}
