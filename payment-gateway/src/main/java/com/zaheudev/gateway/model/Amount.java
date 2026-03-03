package com.zaheudev.gateway.model;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor
@AllArgsConstructor
public class Amount {
    // I am still thinking what to choose between BigDecimal and Long. I will go with Long for now, but I might change it to BigDecimal later if I need more precision.
    // We may need to refactor to use only on Data Type, it get's confusing to have both BigDecimal and Long in the same class.
    private Long amount;
    private String currency;
    /**
     * Creates a new Amount with the specified value and currency.
     *
     * @param amount the monetary value in smallest currency unit (e.g., cents)
     * @param currency the currency code (ISO 4217)
     * @return a new Amount instance
     * @throws IllegalArgumentException if amount is null or negative, or currency is null/blank
     */
    public static Amount of(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-null and non-negative");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency must not be null or empty");
        }

        return Amount.builder()
                .amount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to smallest currency unit (e.g., cents)
                .currency(currency.trim().toUpperCase())
                .build();
    }

    /**
     * Adds the specified amount to this amount.
     *
     * @param other the amount to add
     * @return a new Amount representing the sum
     * @throws IllegalArgumentException if the currencies don't match
     */
    public Amount add(Amount other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return Amount.of(BigDecimal.valueOf(this.amount).add(BigDecimal.valueOf(other.amount)).divide(BigDecimal.valueOf(100)), this.currency);
    }

    /**
     * Subtracts the specified amount from this amount.
     *
     * @param other the amount to subtract
     * @return a new Amount representing the difference
     * @throws IllegalArgumentException if the currencies don't match or result would be negative
     */
    public Amount subtract(Amount other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract amounts with different currencies");
        }
        return Amount.of(BigDecimal.valueOf(this.amount).subtract(BigDecimal.valueOf(other.amount)).divide(BigDecimal.valueOf(100)), this.currency);
    }

}
