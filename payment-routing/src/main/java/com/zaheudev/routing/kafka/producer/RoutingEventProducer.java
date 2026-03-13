package com.zaheudev.routing.kafka.producer;

import com.zaheudev.shared.avro.RoutedCompletedEvent;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoutingEventProducer {
    private final String TOPIC_NAME = "routing-completed";

    private final KafkaTemplate<String, RoutedCompletedEvent> kafkaTemplate;

    public RoutingEventProducer(KafkaTemplate<String, RoutedCompletedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRoutingDecisionEvent(RoutedCompletedEvent event) {
        kafkaTemplate.send(TOPIC_NAME, event.getPaymentId().toString(), event)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        System.out.println("Error publishing routing decision event: " + e.getMessage());
                    }else {
                        System.out.println("Routing decision event published successfully to topic " + TOPIC_NAME);
                    }
                });
    }
}
