package com.zaheudev.gateway.service;

import com.zaheudev.gateway.dto.CreatePaymentRequest;
import com.zaheudev.gateway.dto.PaymentResponse;
import com.zaheudev.gateway.dto.RefundRequest;
import com.zaheudev.shared.dto.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {
    /** Create a new payment */
    PaymentResponse createPayment(CreatePaymentRequest request);

    /** Returns a payment after it's id */
    PaymentResponse getPayment(String paymentId);

    List<PaymentResponse> getAllPayments(int limit);

    /** Cancel an authorized payment */
    PaymentResponse voidPayment(String paymentId);

    /** Refund a captured payment */
    PaymentResponse refundPayment(String paymentId, RefundRequest amount);

    /** Capture a previously authorized payment */
    PaymentResponse capturePayment(String paymentId);

    void updatePaymentStatus(String paymentId, PaymentStatus status, PaymentStatus previousStatus);

    PaymentStatus getPaymentStatus(String paymentId);

}
