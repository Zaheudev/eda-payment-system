package com.zaheudev.demo.service;

import com.zaheudev.demo.dto.CreatePaymentRequest;
import com.zaheudev.demo.dto.PaymentResponse;
import com.zaheudev.demo.entity.PaymentEntity;
import com.zaheudev.demo.kafka.producer.PaymentEventProducer;
import com.zaheudev.demo.model.Amount;
import com.zaheudev.demo.model.Payment;
import com.zaheudev.demo.model.PaymentStatus;
import com.zaheudev.demo.repository.PaymentRepository;
import com.zaheudev.shared.avro.CardDetails;
import com.zaheudev.shared.avro.PaymentRequestedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentServiceImpl implements PaymentService{
    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Payment payment = Payment.createPayment(
                request.getMerchantReference(),
                Amount.of(request.getAmount(), request.getCurrency()),
                request.getCardDetails()
        );
        PaymentEntity entity = PaymentEntity.fromPayment(payment);
        paymentRepository.save(entity);

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
                .setTimestamp(Long.parseLong(payment.getCreatedAt().toString()))
                .setTokenRef(payment.getTokenRef())
                .build();

        PaymentEventProducer producer = new PaymentEventProducer();
        producer.publishPaymentRequestedEvent(paymentRequestedEvent);
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
