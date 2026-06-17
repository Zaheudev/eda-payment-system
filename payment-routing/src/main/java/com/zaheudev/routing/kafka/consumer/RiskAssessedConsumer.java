package com.zaheudev.routing.kafka.consumer;

import com.zaheudev.routing.dto.RoutingResult;
import com.zaheudev.routing.entity.RoutingDecisionEntity;
import com.zaheudev.routing.kafka.producer.RoutingEventProducer;
import com.zaheudev.routing.repository.RoutingDecisionRepository;
import com.zaheudev.routing.service.RoutingService;
import com.zaheudev.shared.avro.PaymentMethodEnum;
import com.zaheudev.shared.avro.RiskAssessedEvent;
import com.zaheudev.shared.avro.RoutedCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

@Slf4j
@Component
public class RiskAssessedConsumer {
    @Autowired
    private RoutingService routingService;

    @Autowired
    private RoutingDecisionRepository routingDecisionRepository;
    @Autowired
    private RoutingEventProducer routingEventProducer;

    @KafkaListener(topics = "risk-assessed")
    public void consume(ConsumerRecord<String, RiskAssessedEvent> record, Acknowledgment ack) {
        String paymentId = record.key();
        RiskAssessedEvent event = record.value();

        log.info("Processing Routing for payment: {}", paymentId);
        System.out.println("Received risk assessment for payment: " + paymentId + " with risk level: " + event.getRiskLevel());

        try {
            RoutingResult result = routingService.calculateOptimalRouting(event);
            Set<PaymentMethodEnum> availableNetworks = routingService.determineAvailableNetworks(event);
            if (!result.hasValidOption()) {
                log.error("No valid option found for payment: {}", paymentId);
                ack.acknowledge();
                return;
            }
            RoutingDecisionEntity routingDecisionEntity = RoutingDecisionEntity.builder()
                    .paymentId(paymentId)
                    .selectedPaymentMethod(result.getSelectedPaymentMethod())
                    .calculatedFee(result.getCalculatedFee())
                    .useToken(result.getUseToken())
                    .availableNetworks(String.valueOf(availableNetworks))
                    .createdAt(LocalDateTime.now()).build();
            routingDecisionRepository.save(routingDecisionEntity);
            log.info("Routing decision saved in db: {}", routingDecisionEntity);

            RoutedCompletedEvent routedCompletedEvent = RoutedCompletedEvent.newBuilder()
                    .setPaymentId(paymentId)
                    .setSelectedPaymentMethod(result.getSelectedPaymentMethod())
                    .setAmount(event.getAmount())
                    .setCardRecord(event.getCardRecord())
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
