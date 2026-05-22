package com.zaheudev.emulator.service;

import com.zaheudev.shared.avro.*;
import com.zaheudev.emulator.entity.EmulatedTransactionEntity;

import java.math.BigDecimal;

public interface CardProcessor {
    AuthorizationCompletedEvent authorize(String paymentId, CardRecord cardRecord,
                                          PaymentMethodEnum selectedPaymentMethod,
                                          BigDecimal amount, String currency);
    CaptureCompletedEvent capture(EmulatedTransactionEntity transactionEntity);
    RefundCompletedEvent refund(String paymentId, String processorTransactionId, BigDecimal refundAmount, String currency);
    AuthorizationCompletedEvent voidTransaction(String paymentId);
}
