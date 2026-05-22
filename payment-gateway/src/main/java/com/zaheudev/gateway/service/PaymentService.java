package com.zaheudev.gateway.service;

import com.zaheudev.gateway.dto.CreatePaymentRequest;
import com.zaheudev.gateway.dto.PaymentResponse;
import com.zaheudev.gateway.dto.RefundRequest;
import com.zaheudev.shared.dto.PaymentStatus;

import java.math.BigDecimal;

public interface PaymentService {
    /** Create a new payment */
    PaymentResponse createPayment(CreatePaymentRequest request);

    /** Returns a payment after it's id */
    PaymentResponse getPayment(String paymentId);

    /** Cancel a payment */
    PaymentResponse cancelPayment(String paymentId);

    /** Refund a captured payment */
    PaymentResponse refundPayment(String paymentId, RefundRequest amount);

    /** Capture a previously authorized payment */
    PaymentResponse capturePayment(String paymentId);

    PaymentStatus getPaymentStatus(String paymentId);

}
