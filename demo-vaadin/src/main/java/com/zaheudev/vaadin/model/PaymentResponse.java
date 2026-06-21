package com.zaheudev.vaadin.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PaymentResponse(
        String paymentId,
        String paymentStatus,
        String rrn,
        String authCode,
        String processorTransactionId,
        String captureId,
        @JsonProperty("amount") Amount amount,
        @JsonProperty("refundedAmount") BigDecimal refundedAmount,
        String createdAt,
        String message
) {
    public record Amount(
            @JsonProperty("amount") long amount,
            @JsonProperty("currency") String currency
    ) {}
}
