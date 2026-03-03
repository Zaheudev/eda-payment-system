package com.zaheudev.gateway.service;

import com.zaheudev.gateway.dto.CreatePaymentRequest;
import com.zaheudev.gateway.dto.PaymentResponse;
import com.zaheudev.gateway.entity.PaymentEntity;
import com.zaheudev.gateway.kafka.producer.PaymentEventProducer;
import com.zaheudev.gateway.model.Amount;
import com.zaheudev.gateway.model.Payment;
import com.zaheudev.gateway.model.PaymentStatus;
import com.zaheudev.gateway.repository.PaymentRepository;
import com.zaheudev.shared.avro.PaymentRequestedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentServiceImpl implements PaymentService{
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    PaymentEventProducer producer;

    // momentna stocam peste tot detaliile cardului netokenizate pentru debugging. In viitor o sa existe
    // un modul special care va tokeniza cardul.
    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Payment payment = Payment.createPayment(
                request.getMerchantReference(),
                Amount.of(request.getAmount(), request.getCurrency()),
                request.getCardDetails()
        );
        PaymentEntity entity = PaymentEntity.fromPayment(payment);
        paymentRepository.save(entity);
        System.out.println("payment saved in db");

        PaymentRequestedEvent paymentRequestedEvent = PaymentRequestedEvent.newBuilder()
                .setPaymentId(payment.getPaymentId())
                .setAmount(com.zaheudev.shared.avro.Amount.newBuilder()
                        .setValue(payment.getAmount().getAmount())
                        .setCurrency(payment.getAmount().getCurrency())
                        .build())
                .setCardDetails(com.zaheudev.shared.avro.CardDetails.newBuilder()
                        .setCardNumber(payment.getCardDetails().getCardNumber())
                        .setExpiryMonth(Integer.parseInt(payment.getCardDetails().getExpiryMonth()))
                        .setExpiryYear(Integer.parseInt(payment.getCardDetails().getExpiryYear()))
                        .setCvv(payment.getCardDetails().getCvv())
                        .build())
                .setStatus(com.zaheudev.shared.avro.PaymentStatus.PENDING)
                .setTimestamp(payment.getCreatedAt().toInstant(java.time.ZoneOffset.UTC).toEpochMilli())
                .setTokenRef(payment.getTokenRef())
                .build();

        producer.publishPaymentRequestedEvent(paymentRequestedEvent);
        System.out.println("payment requested event published");
        return PaymentResponse.create(payment);
    }

    @Override
    public PaymentResponse authorizePayment(String paymentId) {
        return null;
    }

    @Override
    public PaymentResponse getPayment(String paymentId) {
        return null;
    }

    @Override
    public PaymentResponse modifyPayment(String paymentId, BigDecimal newAmount, String status) {
        return null;
    }

    @Override
    public PaymentResponse cancelPayment(String paymentId) {
        return null;
    }

    @Override
    public PaymentResponse refundPayment(String paymentId) {
        return null;
    }

    @Override
    public PaymentResponse capturePayment(String paymentId) {
        return null;
    }

    @Override
    public PaymentStatus getPaymentStatus(String paymentId) {
        return null;
    }
}
