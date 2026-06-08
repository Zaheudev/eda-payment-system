package com.zaheudev.emulator.service;

import com.zaheudev.emulator.client.CardMetadataClient;
import com.zaheudev.emulator.entity.EmulatedTransactionEntity;
import com.zaheudev.emulator.model.TransactionStatus;
import com.zaheudev.emulator.repository.EmulatedTransactionRepository;
import com.zaheudev.shared.avro.*;
import com.zaheudev.shared.dto.CardTokenMetadata;
import com.zaheudev.shared.dto.TokenStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmulatedCardProcessorTest {

    @Mock
    private CardMetadataClient cardMetadataClient;

    @Mock
    private EmulatedTransactionRepository transactionRepository;

    @InjectMocks
    private EmulatedCardProcessor processor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(processor, "minLatency", 0);
        ReflectionTestUtils.setField(processor, "maxLatency", 1);
    }

    private CardRecord.Builder cardRecordBuilder(String tokenRef) {
        return CardRecord.newBuilder()
                .setTokenRef(tokenRef)
                .setTokenValue("tk-123")
                .setBin("411111")
                .setLastFour("1111")
                .setPrimaryNetwork(PaymentMethodEnum.VISA)
                .setCardType("CREDIT")
                .setTokenStatus("ACTIVE");
    }

    @Test
    void isCardValidShouldReturnTrueForFutureExpiry() {
        int futureYear = LocalDate.now().getYear() + 1;
        CardTokenMetadata metadata = CardTokenMetadata.builder()
                .expiryYear(String.valueOf(futureYear))
                .expiryMonth("12")
                .build();
        when(cardMetadataClient.getMetadata("TKN-abc")).thenReturn(metadata);

        CardRecord card = cardRecordBuilder("TKN-abc").build();
        assertThat(processor.isCardValid(card)).isTrue();
    }

    @Test
    void isCardValidShouldReturnFalseForPastExpiry() {
        CardTokenMetadata metadata = CardTokenMetadata.builder()
                .expiryYear("2020")
                .expiryMonth("01")
                .build();
        when(cardMetadataClient.getMetadata("TKN-abc")).thenReturn(metadata);

        CardRecord card = cardRecordBuilder("TKN-abc").build();
        assertThat(processor.isCardValid(card)).isFalse();
    }

    @Test
    void isCardValidShouldReturnFalseOnException() {
        when(cardMetadataClient.getMetadata(anyString())).thenThrow(new RuntimeException("down"));

        CardRecord card = cardRecordBuilder("TKN-abc").build();
        assertThat(processor.isCardValid(card)).isFalse();
    }

    @Test
    void isTokenActiveShouldReturnTrueForActive() {
        when(cardMetadataClient.getTokenStatus("TKN-abc")).thenReturn(TokenStatus.ACTIVE);

        CardRecord card = cardRecordBuilder("TKN-abc").build();
        assertThat(processor.isTokenActive(card)).isTrue();
    }

    @Test
    void isTokenActiveShouldReturnFalseForExpired() {
        when(cardMetadataClient.getTokenStatus("TKN-abc")).thenReturn(TokenStatus.EXPIRED);

        CardRecord card = cardRecordBuilder("TKN-abc").build();
        assertThat(processor.isTokenActive(card)).isFalse();
    }

    @Test
    void isTokenActiveShouldReturnFalseOnException() {
        when(cardMetadataClient.getTokenStatus("TKN-abc")).thenThrow(new RuntimeException("down"));

        CardRecord card = cardRecordBuilder("TKN-abc").build();
        assertThat(processor.isTokenActive(card)).isFalse();
    }

    @Test
    void authorizeShouldReturnFailureForExpiredCard() {
        CardTokenMetadata metadata = CardTokenMetadata.builder()
                .expiryYear("2020").expiryMonth("01").build();
        when(cardMetadataClient.getMetadata("TKN-abc")).thenReturn(metadata);

        RoutedCompletedEvent event = RoutedCompletedEvent.newBuilder()
                .setPaymentId("PMT001")
                .setSelectedPaymentMethod(PaymentMethodEnum.VISA)
                .setEstimatedCost("1.00")
                .setUseToken(false)
                .setAvailableNetworks(java.util.List.of())
                .setCardRecord(cardRecordBuilder("TKN-abc").build())
                .setAmount(Amount.newBuilder().setValue(100L).setCurrency("USD").build())
                .setTimestamp(System.currentTimeMillis())
                .build();

        AuthorizationCompletedEvent result = processor.authorize(event);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Card expired");
    }

    @Test
    void authorizeShouldReturnFailureForInactiveToken() {
        when(cardMetadataClient.getTokenStatus("TKN-abc")).thenReturn(TokenStatus.EXPIRED);

        RoutedCompletedEvent event = RoutedCompletedEvent.newBuilder()
                .setPaymentId("PMT001")
                .setSelectedPaymentMethod(PaymentMethodEnum.VISA)
                .setEstimatedCost("1.00")
                .setUseToken(true)
                .setAvailableNetworks(java.util.List.of())
                .setCardRecord(cardRecordBuilder("TKN-abc").build())
                .setAmount(Amount.newBuilder().setValue(100L).setCurrency("USD").build())
                .setTimestamp(System.currentTimeMillis())
                .build();

        AuthorizationCompletedEvent result = processor.authorize(event);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Token expired");
    }

    @Test
    void captureShouldMapEntityFields() {
        EmulatedTransactionEntity entity = EmulatedTransactionEntity.builder()
                .paymentId("PMT001")
                .processorTransactionId("PROC-001")
                .rrn("ABC123")
                .authCode("123456")
                .authorizedAmount(BigDecimal.valueOf(99.99))
                .currency("USD")
                .networkFee(BigDecimal.valueOf(0.50))
                .build();

        CaptureCompletedEvent result = processor.capture(entity);

        assertThat(result.getPaymentId()).isEqualTo("PMT001");
        assertThat(result.getProcessorTransactionId()).isEqualTo("PROC-001");
        assertThat(result.getRrn()).isEqualTo("ABC123");
        assertThat(result.getAuthCode()).isEqualTo("123456");
        assertThat(result.getAmount().getValue()).isEqualTo(99L);
        assertThat(result.getNetworkFee()).isEqualTo("0.5");
    }

    @Test
    void refundShouldBuildEventWithCorrectStructure() {
        RefundCompletedEvent result = processor.refund("PMT001", "PROC-001", BigDecimal.valueOf(50.00), "USD");

        assertThat(result.getPaymentId()).isEqualTo("PMT001");
        assertThat(result.getOriginalProcessorTransactionId()).isEqualTo("PROC-001");
        assertThat(result.getRefundId()).startsWith("REFUND-");
        assertThat(result.getRefundRrn()).isNotNull();
        assertThat(result.getRefundAuthCode()).isNotNull();
    }

    @Test
    void voidTransactionShouldFailWhenNotAuthorized() {
        EmulatedTransactionEntity entity = EmulatedTransactionEntity.builder()
                .paymentId("PMT001")
                .processorTransactionId("PROC-001")
                .transactionStatus(TransactionStatus.CAPTURED)
                .build();
        when(transactionRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        VoidCompletedEvent result = processor.voidTransaction("PMT001");

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Only authorized");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void voidTransactionShouldSucceedWhenAuthorized() {
        EmulatedTransactionEntity entity = EmulatedTransactionEntity.builder()
                .paymentId("PMT001")
                .processorTransactionId("PROC-001")
                .transactionStatus(TransactionStatus.AUTHORIZED)
                .build();
        when(transactionRepository.findByPaymentId("PMT001")).thenReturn(Optional.of(entity));

        VoidCompletedEvent result = processor.voidTransaction("PMT001");

        assertThat(result.getPaymentId()).isEqualTo("PMT001");
        verify(transactionRepository).save(any());
    }

    @Test
    void generateRRNShouldBeCorrectLengthAndCharset() {
        String rrn = processor.generateRRN();
        assertThat(rrn).hasSize(12);
        String allowed = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (char c : rrn.toCharArray()) {
            assertThat(allowed).contains(String.valueOf(c));
        }
    }
}
