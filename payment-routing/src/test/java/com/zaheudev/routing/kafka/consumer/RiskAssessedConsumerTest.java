package com.zaheudev.routing.kafka.consumer;

import com.zaheudev.routing.dto.RoutingResult;
import com.zaheudev.routing.kafka.producer.RoutingEventProducer;
import com.zaheudev.routing.repository.RoutingDecisionRepository;
import com.zaheudev.routing.service.RoutingService;
import com.zaheudev.shared.avro.PaymentMethodEnum;
import com.zaheudev.shared.avro.RiskAssessedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskAssessedConsumerTest {

    @Mock
    private RoutingService routingService;

    @Mock
    private RoutingDecisionRepository routingDecisionRepository;

    @Mock
    private RoutingEventProducer routingEventProducer;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private RiskAssessedConsumer consumer;

    @Test
    void consumeShouldSaveAndPublishWhenValidOption() {
        RoutingResult result = RoutingResult.builder()
                .selectedPaymentMethod(PaymentMethodEnum.VISA)
                .calculatedFee(BigDecimal.valueOf(2.50))
                .useToken(true)
                .build();
        Set<PaymentMethodEnum> networks = Set.of(PaymentMethodEnum.VISA);
        when(routingService.calculateOptimalRouting(any())).thenReturn(result);
        when(routingService.determineAvailableNetworks(any())).thenReturn(networks);

        RiskAssessedEvent event = RiskAssessedEvent.newBuilder()
                .setAssessmentId("ASM-001")
                .setPaymentId("PMT001")
                .setRiskLevel(com.zaheudev.shared.avro.RiskLevel.LOW)
                .setReason("ok")
                .setApproved(true)
                .setTimestamp(System.currentTimeMillis())
                .build();
        ConsumerRecord<String, RiskAssessedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consume(record, ack);

        verify(routingDecisionRepository).save(any());
        verify(routingEventProducer).publishRoutingDecisionEvent(any());
        verify(ack).acknowledge();
    }

    @Test
    void consumeShouldOnlyAckWhenNoValidOption() {
        RoutingResult result = RoutingResult.noValidOptions(BigDecimal.TEN, "USD");
        when(routingService.calculateOptimalRouting(any())).thenReturn(result);

        RiskAssessedEvent event = RiskAssessedEvent.newBuilder()
                .setAssessmentId("ASM-001")
                .setPaymentId("PMT001")
                .setRiskLevel(com.zaheudev.shared.avro.RiskLevel.LOW)
                .setReason("ok")
                .setApproved(true)
                .setTimestamp(System.currentTimeMillis())
                .build();
        ConsumerRecord<String, RiskAssessedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consume(record, ack);

        verify(routingDecisionRepository, never()).save(any());
        verify(routingEventProducer, never()).publishRoutingDecisionEvent(any());
        verify(ack).acknowledge();
    }
}
