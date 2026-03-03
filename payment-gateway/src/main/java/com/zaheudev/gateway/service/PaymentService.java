package com.zaheudev.gateway.service;

import com.zaheudev.gateway.dto.CreatePaymentRequest;
import com.zaheudev.gateway.dto.PaymentResponse;
import com.zaheudev.gateway.model.PaymentStatus;

import java.math.BigDecimal;

public interface PaymentService {
    /** Create a new payment */
    PaymentResponse createPayment(CreatePaymentRequest request);

    /** Authorize a payment */
    PaymentResponse authorizePayment(String paymentId);

    /** Returns a payment after it's id */
    PaymentResponse getPayment(String paymentId);

    /** Modify the amount of an ongoing payment */
    PaymentResponse modifyPayment(String paymentId, BigDecimal newAmount, String status);

    /** Cancel a payment */
    PaymentResponse cancelPayment(String paymentId);

    /** Refund a captured payment */
    PaymentResponse refundPayment(String paymentId);

    /** Capture a previously authorized payment */
    PaymentResponse capturePayment(String paymentId);

    PaymentStatus getPaymentStatus(String paymentId);

}
