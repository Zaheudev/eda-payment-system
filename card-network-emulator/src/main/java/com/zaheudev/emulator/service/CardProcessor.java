package com.zaheudev.emulator.service;

import com.zaheudev.emulator.entity.EmulatedTransactionEntity;
import com.zaheudev.shared.avro.AuthorizationCompleted;
import com.zaheudev.shared.avro.CardRecord;
import com.zaheudev.shared.avro.PaymentMethodEnum;

import java.math.BigDecimal;

public interface CardProcessor {
    AuthorizationCompleted authorize(String paymentId, CardRecord cardRecord,
                                     PaymentMethodEnum selectedPaymentMethod,
                                     BigDecimal amount, String currency);
    AuthorizationCompleted capture(String paymentId);
    AuthorizationCompleted voidTransaction(String paymentId);
    AuthorizationCompleted refund(String paymentId);
}
