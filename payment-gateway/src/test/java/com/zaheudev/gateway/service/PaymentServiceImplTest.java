package com.zaheudev.gateway.service;

import com.zaheudev.gateway.client.TokenizerClient;
import com.zaheudev.gateway.dto.CardDetails;
import com.zaheudev.gateway.dto.CreatePaymentRequest;
import com.zaheudev.gateway.dto.PaymentResponse;
import com.zaheudev.gateway.dto.RefundRequest;
import com.zaheudev.gateway.entity.PaymentEntity;
import com.zaheudev.gateway.exception.PaymentFailedException;
import com.zaheudev.gateway.kafka.producer.PaymentEventProducer;
import com.zaheudev.gateway.repository.PaymentRepository;
import com.zaheudev.shared.dto.PaymentStatus;
import com.zaheudev.shared.dto.TokenStatus;
import com.zaheudev.shared.dto.TokenizeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventProducer producer;

    @Mock
    private TokenizerClient tokenizerClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private CardDetails cardDetails;
    private TokenizeResponse tokenizeResponse;

    @BeforeEach
    void setUp() {
        cardDetails = new CardDetails("4111111111111111", "12", "2027", "John Doe", "123");
        tokenizeResponse = TokenizeResponse.builder()
                .tokenRef("TKN-abc")
                .tokenValue("tk-123")
                .bin("411111")
                .lastFour("1111")
                .cardType("CREDIT")
                .status(TokenStatus.ACTIVE)
                .expiryMonth("12")
                .expiryYear("2027")
                .cardholderName("John Doe")
                .build();
    }

    @Test
    void createPaymentShouldSaveEntityAndPublishEvent() {
        when(tokenizerClient.tokenize(any())).thenReturn(tokenizeResponse);
        CreatePaymentRequest request = new CreatePaymentRequest("order-1", BigDecimal.valueOf(99.99), "USD", cardDetails, null);

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response.getPaymentId()).startsWith("PMT");
        assertThat(response.getMessage()).isEqualTo("Payment request is created");
        verify(paymentRepository).save(any(PaymentEntity.class));
        verify(producer).publishPaymentRequestedEvent(any());
    }

    @Test
    void capturePaymentShouldPublishWhenAuthorized() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.AUTHORIZED);
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        PaymentResponse response = paymentService.capturePayment("PMT001");

        verify(producer).publishCaptureRequestedEvent(any());
        assertThat(response.getMessage()).isEqualTo("Request capture sent successfully");
    }

    @Test
    void capturePaymentShouldThrowWhenNotAuthorized() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.CREATED);
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> paymentService.capturePayment("PMT001"))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void voidPaymentShouldPublishWhenAuthorized() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.AUTHORIZED);
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        PaymentResponse response = paymentService.voidPayment("PMT001");

        verify(producer).publishVoidRequestedEvent(any());
        assertThat(response.getMessage()).isEqualTo("Request void sent successfully");
    }

    @Test
    void voidPaymentShouldThrowWhenNotAuthorized() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.CREATED);
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> paymentService.voidPayment("PMT001"))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void refundPaymentShouldPublishWhenCaptured() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.CAPTURED);
        entity.setAmount(BigDecimal.valueOf(100));
        entity.setCurrency("USD");
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));
        RefundRequest refundRequest = RefundRequest.builder()
                .amount(BigDecimal.valueOf(50))
                .currency("USD")
                .build();

        PaymentResponse response = paymentService.refundPayment("PMT001", refundRequest);

        assertThat(response).isNull();
        verify(producer).publishRefundRequestedEvent(any());
    }

    @Test
    void refundPaymentShouldPublishWhenPartiallyRefunded() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.PARTIALLY_REFUNDED);
        entity.setAmount(BigDecimal.valueOf(100));
        entity.setCurrency("USD");
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));
        RefundRequest refundRequest = RefundRequest.builder()
                .amount(BigDecimal.valueOf(25))
                .currency("USD")
                .build();

        PaymentResponse response = paymentService.refundPayment("PMT001", refundRequest);

        assertThat(response).isNull();
        verify(producer).publishRefundRequestedEvent(any());
    }

    @Test
    void refundPaymentShouldThrowWhenNotCaptured() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.AUTHORIZED);
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> paymentService.refundPayment("PMT001", RefundRequest.builder().build()))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("not captured");
    }

    @Test
    void updatePaymentStatusShouldTransitionFromPredecessor() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.CREATED);
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        paymentService.updatePaymentStatus("PMT001", PaymentStatus.RISK_ASSESSED, PaymentStatus.CREATED);

        assertThat(entity.getStatus()).isEqualTo(PaymentStatus.RISK_ASSESSED);
        verify(paymentRepository).save(entity);
    }

    @Test
    void updatePaymentStatusShouldTransitionFromPending() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.PENDING);
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        paymentService.updatePaymentStatus("PMT001", PaymentStatus.RISK_ASSESSED, PaymentStatus.CREATED);

        assertThat(entity.getStatus()).isEqualTo(PaymentStatus.RISK_ASSESSED);
        verify(paymentRepository).save(entity);
    }

    @Test
    void updatePaymentStatusShouldSkipWhenAtOrPastTarget() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.AUTHORIZED);
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        paymentService.updatePaymentStatus("PMT001", PaymentStatus.RISK_ASSESSED, PaymentStatus.CREATED);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void updatePaymentStatusShouldThrowOnInvalidTransition() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.AUTHORIZED);
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> paymentService.updatePaymentStatus("PMT001", PaymentStatus.CAPTURED, PaymentStatus.RISK_ASSESSED))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    void getPaymentEntityShouldThrowWhenNotFound() {
        when(paymentRepository.findByPaymentId("NONEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentEntity("NONEXIST"))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getResponseEntityShouldReturn404ForNullEntity() {
        ResponseEntity<PaymentResponse> resp = paymentService.getResponseEntity(200, null);
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        assertThat(resp.getBody()).isNull();
    }

    @Test
    void getResponseEntityShouldReturnGivenStatus() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.AUTHORIZED);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setAmount(BigDecimal.valueOf(100));
        entity.setCurrency("USD");

        ResponseEntity<PaymentResponse> resp = paymentService.getResponseEntity(202, entity);
        assertThat(resp.getStatusCode().value()).isEqualTo(202);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getPaymentId()).isEqualTo("PMT001");
    }

    @Test
    void getAllPaymentsShouldClampLimit() {
        when(paymentRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of());

        paymentService.getAllPayments(0);
        ArgumentCaptor<org.springframework.data.domain.PageRequest> captor = ArgumentCaptor.forClass(org.springframework.data.domain.PageRequest.class);
        verify(paymentRepository).findAllByOrderByCreatedAtDesc(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);

        paymentService.getAllPayments(200);
        verify(paymentRepository, times(2)).findAllByOrderByCreatedAtDesc(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void updateNetworkFeeShouldSetAndSave() {
        PaymentEntity entity = buildEntity("PMT001", PaymentStatus.AUTHORIZED);
        when(paymentRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        paymentService.updateNetworkFee("PMT001", BigDecimal.valueOf(2.50));

        assertThat(entity.getNetworkFee()).isEqualByComparingTo(BigDecimal.valueOf(2.50));
        verify(paymentRepository).save(entity);
    }

    private PaymentEntity buildEntity(String paymentId, PaymentStatus status) {
        return PaymentEntity.builder()
                .paymentId(paymentId)
                .status(status)
                .amount(BigDecimal.valueOf(99.99))
                .currency("USD")
                .merchantRef("order-1")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
