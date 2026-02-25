package com.zaheudev.demo.model;

//i will come later with docs, just want to have the code in place for now
public enum PaymentStatus {
    CREATED,
    AUTHORIZED,
    CAPTURED,
    PARTIALLY_CAPTURED,
    PENDING,
    ROUTING_COMPLETED,
    RISK_ASSESSED,
    CANCELLED,
    REFUNDED,
    SUCCESS,
    FAILED
}
