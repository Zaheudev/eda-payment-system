package com.zaheudev.risk.kafka.consumer;

import com.zaheudev.risk.kafka.producer.RiskFraudProducer;
import com.zaheudev.risk.model.RiskLevel;
import com.zaheudev.risk.repository.RiskAssessmentRepository;
import com.zaheudev.risk.service.RiskService;
import com.zaheudev.shared.avro.PaymentRequestedEvent;
import com.zaheudev.shared.avro.RiskAssessedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRequestedConsumerTest {

    @Mock
    private RiskService riskService;

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private RiskFraudProducer producer;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private PaymentRequestedConsumer consumer;

    private PaymentRequestedEvent buildPaymentRequestedEvent(String paymentId) {
        return PaymentRequestedEvent.newBuilder()
                .setPaymentId(paymentId)
                .setAmount(com.zaheudev.shared.avro.Amount.newBuilder().setValue(10000L).setCurrency("USD").build())
                .setStatus(com.zaheudev.shared.avro.PaymentStatus.PENDING)
                .setMerchantRef("order-1")
                .setCardRecord(com.zaheudev.shared.avro.CardRecord.newBuilder()
                        .setTokenRef("TKN-abc")
                        .setTokenValue("tk-123")
                        .setBin("411111")
                        .setLastFour("1111")
                        .setPrimaryNetwork(com.zaheudev.shared.avro.PaymentMethodEnum.VISA)
                        .setCardType("CREDIT")
                        .setTokenStatus("ACTIVE")
                        .build())
                .setTimestamp(System.currentTimeMillis())
                .build();
    }

    @Test
    void consumeShouldPublishRiskAssessedWhenApproved() {
        when(riskService.assessRisk("PMT001")).thenReturn(RiskLevel.LOW);
        PaymentRequestedEvent event = buildPaymentRequestedEvent("PMT001");
        ConsumerRecord<String, PaymentRequestedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consume(record, ack);

        verify(riskAssessmentRepository).save(any());
        verify(producer).publishRiskAssessmentEvent(any(RiskAssessedEvent.class));
        verify(producer, never()).publishPaymentRejectedEvent(any());
        verify(ack, atLeastOnce()).acknowledge();
    }

    @Test
    void consumeShouldPublishRejectedWhenCritical() {
        when(riskService.assessRisk("PMT001")).thenReturn(RiskLevel.CRITICAL);
        PaymentRequestedEvent event = buildPaymentRequestedEvent("PMT001");
        ConsumerRecord<String, PaymentRequestedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consume(record, ack);

        verify(riskAssessmentRepository).save(any());
        verify(producer).publishPaymentRejectedEvent(any());
        verify(producer, never()).publishRiskAssessmentEvent(any());
        verify(ack, atLeastOnce()).acknowledge();
    }

    @Test
    void consumeMediumRiskShouldBeApproved() {
        when(riskService.assessRisk("PMT001")).thenReturn(RiskLevel.MEDIUM);
        PaymentRequestedEvent event = buildPaymentRequestedEvent("PMT001");
        ConsumerRecord<String, PaymentRequestedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consume(record, ack);

        verify(producer).publishRiskAssessmentEvent(any());
        verify(producer, never()).publishPaymentRejectedEvent(any());
    }

    @Test
    void consumeHighRiskShouldBeApproved() {
        when(riskService.assessRisk("PMT001")).thenReturn(RiskLevel.HIGH);
        PaymentRequestedEvent event = buildPaymentRequestedEvent("PMT001");
        ConsumerRecord<String, PaymentRequestedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consume(record, ack);

        verify(producer).publishRiskAssessmentEvent(any());
        verify(producer, never()).publishPaymentRejectedEvent(any());
    }
}
