package com.zaheudev.vaadin.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentResponse(
        String paymentId,
        String paymentStatus,
        String rrn,
        String authCode,
        String processorTransactionId,
        String captureId,
        @JsonProperty("amount") Amount amount,
        String createdAt,
        String message
) {
    public record Amount(
            @JsonProperty("amount") long amount,
            @JsonProperty("currency") String currency
    ) {}
}
