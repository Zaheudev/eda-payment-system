package com.zaheudev.demo.kafka.producer;

import com.zaheudev.shared.avro.PaymentRequestedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {
    private final String TOPIC_NAME = "payment-requests";

    @Autowired
    private KafkaTemplate<String, PaymentRequestedEvent> kafkaTemplate;

    public void publishPaymentRequestedEvent(PaymentRequestedEvent event) {
        kafkaTemplate.send(TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.err.println("Failed to publish event: " + e.getMessage());
                    } else {
                        System.out.println("Event published successfully to topic " + TOPIC_NAME);
                    }
                });
    }
}
