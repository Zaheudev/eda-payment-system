package com.zaheudev.gateway.kafka.consumer;

import com.zaheudev.gateway.entity.PaymentEntity;
import com.zaheudev.gateway.repository.PaymentRepository;
import com.zaheudev.gateway.service.PaymentServiceImpl;
import com.zaheudev.shared.avro.*;
import com.zaheudev.shared.dto.PaymentStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventsConsumerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentServiceImpl paymentService;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private PaymentEventsConsumer consumer;

    @Test
    void consumeAuthorizationCompletedSuccessShouldSetAuthorized() {
        PaymentEntity entity = buildEntity("PMT001");
        when(paymentRepository.findById("PMT001")).thenReturn(Optional.of(entity));

        AuthorizationCompletedEvent event = AuthorizationCompletedEvent.newBuilder()
                .setPaymentId("PMT001")
                .setSuccess(true)
                .setRrn("ABC123")
                .setAuthCode("123456")
                .setProcessorTransactionId("PROC-001")
                .build();
        ConsumerRecord<String, AuthorizationCompletedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consumeAuthorizationCompleted(record, ack);

        org.assertj.core.api.Assertions.assertThat(entity.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        org.assertj.core.api.Assertions.assertThat(entity.getRrn()).isEqualTo("ABC123");
        verify(ack).acknowledge();
    }

    @Test
    void consumeAuthorizationCompletedFailureShouldSetFailed() {
        PaymentEntity entity = buildEntity("PMT001");
        when(paymentRepository.findById("PMT001")).thenReturn(Optional.of(entity));

        AuthorizationCompletedEvent event = AuthorizationCompletedEvent.newBuilder()
                .setPaymentId("PMT001")
                .setSuccess(false)
                .setRrn("")
                .setAuthCode("")
                .setProcessorTransactionId("")
                .setSelectedPaymentMethod(com.zaheudev.shared.avro.PaymentMethodEnum.VISA)
                .setErrorMessage("Declined")
                .setTimestamp(System.currentTimeMillis())
                .build();
        ConsumerRecord<String, AuthorizationCompletedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consumeAuthorizationCompleted(record, ack);

        org.assertj.core.api.Assertions.assertThat(entity.getStatus()).isEqualTo(PaymentStatus.FAILED);
        org.assertj.core.api.Assertions.assertThat(entity.getErrorMessage()).isEqualTo("Declined");
        verify(ack).acknowledge();
    }

    @Test
    void consumePaymentRejectedShouldSetRejected() {
        PaymentEntity entity = buildEntity("PMT001");
        when(paymentRepository.findById("PMT001")).thenReturn(Optional.of(entity));

        PaymentRejectedEvent event = PaymentRejectedEvent.newBuilder()
                .setPaymentId("PMT001")
                .setRiskLevel(com.zaheudev.shared.avro.RiskLevel.CRITICAL)
                .setReason("High risk")
                .setTimestamp(System.currentTimeMillis())
                .build();
        ConsumerRecord<String, PaymentRejectedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consumePaymentRejected(record, ack);

        org.assertj.core.api.Assertions.assertThat(entity.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        verify(ack).acknowledge();
    }

    @Test
    void consumeCaptureCompletedSuccessShouldSetCaptured() {
        PaymentEntity entity = buildEntity("PMT001");
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        CaptureCompletedEvent event = CaptureCompletedEvent.newBuilder()
                .setPaymentId("PMT001")
                .setSuccess(true)
                .setCaptureId("CAP-001")
                .setTimestamp(System.currentTimeMillis())
                .build();
        ConsumerRecord<String, CaptureCompletedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consumeCaptureCompleted(record, ack);

        org.assertj.core.api.Assertions.assertThat(entity.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        verify(ack).acknowledge();
    }

    @Test
    void consumeRefundCompletedSuccessShouldSetRefunded() {
        PaymentEntity entity = buildEntity("PMT001");
        entity.setAmount(BigDecimal.valueOf(100));
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        RefundCompletedEvent event = RefundCompletedEvent.newBuilder()
                .setPaymentId("PMT001")
                .setSuccess(true)
                .setStatus("REFUNDED")
                .setRefundedAmount(Amount.newBuilder().setValue(5000L).setCurrency("USD").build())
                .setTimeStamp(System.currentTimeMillis())
                .build();
        ConsumerRecord<String, RefundCompletedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consumeRefundCompleted(record, ack);

        org.assertj.core.api.Assertions.assertThat(entity.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(ack).acknowledge();
    }

    @Test
    void consumeRefundCompletedFailureShouldOnlyAck() {
        RefundCompletedEvent event = RefundCompletedEvent.newBuilder()
                .setPaymentId("PMT001")
                .setSuccess(false)
                .setErrorMessage("Failed")
                .build();
        ConsumerRecord<String, RefundCompletedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consumeRefundCompleted(record, ack);

        verify(paymentRepository, never()).findByPaymentId(any());
        verify(ack).acknowledge();
    }

    @Test
    void consumeVoidCompletedSuccessShouldSetVoid() {
        PaymentEntity entity = buildEntity("PMT001");
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        VoidCompletedEvent event = VoidCompletedEvent.newBuilder()
                .setPaymentId("PMT001")
                .setSuccess(true)
                .setTimeStamp(System.currentTimeMillis())
                .build();
        ConsumerRecord<String, VoidCompletedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consumeVoidCompleted(record, ack);

        org.assertj.core.api.Assertions.assertThat(entity.getStatus()).isEqualTo(PaymentStatus.VOID);
        verify(ack).acknowledge();
    }

    @Test
    void consumeRiskAssessedShouldUpdateStatus() {
        RiskAssessedEvent event = RiskAssessedEvent.newBuilder()
                .setAssessmentId("ASM-001")
                .setPaymentId("PMT001")
                .setRiskLevel(com.zaheudev.shared.avro.RiskLevel.LOW)
                .setReason("ok")
                .setApproved(true)
                .setTimestamp(System.currentTimeMillis())
                .build();
        ConsumerRecord<String, RiskAssessedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consumeRiskAssessed(record, ack);

        verify(paymentService).updatePaymentStatus("PMT001", PaymentStatus.RISK_ASSESSED, PaymentStatus.CREATED);
        verify(ack, atLeastOnce()).acknowledge();
    }

    @Test
    void consumeRoutingCompletedShouldUpdateStatusAndFee() {
        RoutedCompletedEvent event = RoutedCompletedEvent.newBuilder()
                .setPaymentId("PMT001")
                .setSelectedPaymentMethod(com.zaheudev.shared.avro.PaymentMethodEnum.VISA)
                .setUseToken(false)
                .setEstimatedCost("2.50")
                .setAvailableNetworks(java.util.List.of(com.zaheudev.shared.avro.PaymentMethodEnum.VISA))
                .setTimestamp(System.currentTimeMillis())
                .build();
        ConsumerRecord<String, RoutedCompletedEvent> record = new ConsumerRecord<>("topic", 0, 0, "PMT001", event);

        consumer.consumeRoutingCompleted(record, ack);

        verify(paymentService).updatePaymentStatus("PMT001", PaymentStatus.ROUTING_COMPLETED, PaymentStatus.RISK_ASSESSED);
        verify(paymentService).updateNetworkFee(eq("PMT001"), any(BigDecimal.class));
        verify(ack, atLeastOnce()).acknowledge();
    }

    private PaymentEntity buildEntity(String paymentId) {
        return PaymentEntity.builder()
                .paymentId(paymentId)
                .status(PaymentStatus.CREATED)
                .amount(BigDecimal.valueOf(99.99))
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
