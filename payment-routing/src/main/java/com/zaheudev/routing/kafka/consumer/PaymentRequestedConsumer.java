package com.zaheudev.routing.kafka.consumer;

import com.zaheudev.routing.dto.RoutingResult;
import com.zaheudev.routing.entity.RoutingDecision;
import com.zaheudev.routing.kafka.producer.RoutingEventProducer;
import com.zaheudev.routing.repository.RoutingDecisionRepository;
import com.zaheudev.routing.service.RoutingService;
import com.zaheudev.shared.avro.PaymentMethodEnum;
import com.zaheudev.shared.avro.PaymentRequestedEvent;
import com.zaheudev.shared.avro.RoutedCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

@Slf4j
@Component
public class PaymentRequestedConsumer {
    @Autowired
    private RoutingService routingService;

    @Autowired
    private RoutingDecisionRepository routingDecisionRepository;
    @Autowired
    private RoutingEventProducer routingEventProducer;

    @KafkaListener(topics = "payment-requests")
    public void consume(ConsumerRecord<String, PaymentRequestedEvent> record, Acknowledgment ack) {
        String paymentId = record.key();
        PaymentRequestedEvent event = record.value();

        log.info("Processing Routing for payment: {}", paymentId);

        try {
            RoutingResult result = routingService.calculateOptimalRouting(event);
            Set<PaymentMethodEnum> availableNetworks = routingService.determineAvailableNetworks(event);
            if (!result.hasValidOption()) {
                log.error("No valid option found for payment: {}", paymentId);
                ack.acknowledge();
                return;
            }
            RoutingDecision routingDecision = RoutingDecision.builder()
                    .paymentId(paymentId)
                    .selectedPaymentMethod(result.getSelectedPaymentMethod())
                    .calculatedFee(result.getCalculatedFee())
                    .useToken(result.getUseToken())
                    .createdAt(LocalDateTime.now()).build();

            routingDecisionRepository.save(routingDecision);
            log.info("Routing decision saved in db: {}", routingDecision);

            RoutedCompletedEvent routedCompletedEvent = RoutedCompletedEvent.newBuilder()
                    .setPaymentId(paymentId)
                    .setSelectedPaymentMethod(result.getSelectedPaymentMethod())
                    .setEstimatedCost(result.getCalculatedFee().toString())
                    .setUseToken(result.getUseToken())
                    .setTimestamp(System.currentTimeMillis())
                    .setAvailableNetworks(new ArrayList<>(availableNetworks))
                    .build();

            // post message to kafka
            routingEventProducer.publishRoutingDecisionEvent(routedCompletedEvent);

            ack.acknowledge();
            log.info("Routing result: {}", result);
        } catch (Exception e) {
            log.error("Error processing payment: ", e);
        }

    }
}
