package com.zaheudev.gateway.entity;

import com.zaheudev.gateway.dto.PaymentResponse;
import com.zaheudev.gateway.model.Amount;
import com.zaheudev.gateway.model.Payment;
import com.zaheudev.shared.dto.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEntityTest {

    @Test
    void fromPaymentShouldDivideAmountBy100() {
        Payment payment = Payment.createPayment("ref", Amount.of(BigDecimal.valueOf(99.99), "USD"), null, "tkn");
        PaymentEntity entity = PaymentEntity.fromPayment(payment);

        assertThat(entity.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        assertThat(entity.getCurrency()).isEqualTo("USD");
        assertThat(entity.getPaymentId()).isEqualTo(payment.getPaymentId());
    }

    @Test
    void tranformInPaymentResponseShouldMultiplyAmountBy100() {
        PaymentEntity entity = PaymentEntity.builder()
                .paymentId("PMT001")
                .status(PaymentStatus.AUTHORIZED)
                .amount(BigDecimal.valueOf(50.00))
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponse resp = entity.tranformInPaymentResponse("Success");

        assertThat(resp.getPaymentId()).isEqualTo("PMT001");
        assertThat(resp.getAmount().getAmount()).isEqualTo(5000L);
        assertThat(resp.getPaymentStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    void tranformInPaymentResponseShouldUseErrorMessageWhenPresent() {
        PaymentEntity entity = PaymentEntity.builder()
                .paymentId("PMT002")
                .status(PaymentStatus.FAILED)
                .errorMessage("Declined")
                .amount(BigDecimal.ZERO)
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponse resp = entity.tranformInPaymentResponse("Fallback");
        assertThat(resp.getMessage()).isEqualTo("Declined");
    }

    @Test
    void tranformInPaymentResponseShouldUseFallbackWhenNoError() {
        PaymentEntity entity = PaymentEntity.builder()
                .paymentId("PMT003")
                .status(PaymentStatus.CREATED)
                .amount(BigDecimal.ZERO)
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponse resp = entity.tranformInPaymentResponse("Fallback");
        assertThat(resp.getMessage()).isEqualTo("Fallback");
    }
}
