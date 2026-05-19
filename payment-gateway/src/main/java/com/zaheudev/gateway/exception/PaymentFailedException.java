package com.zaheudev.gateway.exception;

import com.zaheudev.gateway.entity.PaymentEntity;

public class PaymentFailedException extends RuntimeException{
    private final PaymentEntity paymentEntity;

    public PaymentFailedException(PaymentEntity paymentEntity, String message) {
        super(message);
        this.paymentEntity = paymentEntity;
    }

    public PaymentEntity getPaymentEntity() {
        return paymentEntity;
    }
}
