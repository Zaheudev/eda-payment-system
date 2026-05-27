package com.zaheudev.shared.dto;

//i will come later with docs, just want to have the code in place for now
public enum PaymentStatus {
    CREATED,
    AUTHORIZED,
    CAPTURED,
    PARTIALLY_CAPTURED,
    PENDING,
    ROUTING_COMPLETED,
    RISK_ASSESSED,
    PARTIALLY_REFUNDED,
    REFUNDED,
    VOID,
    SUCCESS,
    FAILED,
    REJECTED
}
