package com.zaheudev.gateway.model;

import com.zaheudev.gateway.dto.CardDetails;
import com.zaheudev.shared.dto.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    @Test
    void createPaymentShouldSetCreatedStatusAndTimestamps() {
        CardDetails card = new CardDetails();
        Payment payment = Payment.createPayment("order-1", Amount.of(BigDecimal.TEN, "USD"), card, "TKN-abc");

        assertThat(payment.getPaymentId()).startsWith("PMT");
        assertThat(payment.getMerchantRef()).isEqualTo("order-1");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(payment.getTokenRef()).isEqualTo("TKN-abc");
        assertThat(payment.getCreatedAt()).isNotNull();
        assertThat(payment.getUpdatedAt()).isNotNull();
        assertThat(payment.getCreatedAt()).isEqualTo(payment.getUpdatedAt());
    }

    @Test
    void generatePaymentIdShouldStartWithPMT() {
        String id = Payment.generatePaymentId();
        assertThat(id).startsWith("PMT").hasSize(12);
    }

    @Test
    void updateStatusShouldUpdateStatusAndTimestamp() {
        Payment payment = Payment.createPayment("ref", Amount.of(BigDecimal.ONE, "USD"), new CardDetails(), "tkn");
        payment.updateStatus(PaymentStatus.AUTHORIZED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getUpdatedAt()).isAfter(payment.getCreatedAt());
    }

    @Test
    void authorizeShouldSetAuthorizedStatus() {
        Payment payment = Payment.createPayment("ref", Amount.of(BigDecimal.ONE, "USD"), new CardDetails(), "tkn");
        payment.authorize();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }
}
