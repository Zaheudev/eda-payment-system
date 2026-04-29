package com.zaheudev.gateway.service;

import com.zaheudev.gateway.client.TokenizerClient;
import com.zaheudev.gateway.dto.CreatePaymentRequest;
import com.zaheudev.gateway.dto.PaymentResponse;
import com.zaheudev.gateway.entity.PaymentEntity;
import com.zaheudev.gateway.kafka.producer.PaymentEventProducer;
import com.zaheudev.gateway.model.Amount;
import com.zaheudev.gateway.model.Payment;
import com.zaheudev.gateway.model.PaymentStatus;
import com.zaheudev.gateway.repository.PaymentRepository;
import com.zaheudev.shared.avro.PaymentRequestedEvent;
import com.zaheudev.shared.dto.TokenizeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService{
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventProducer producer;

    @Autowired
    private TokenizerClient tokenizerClient;

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        TokenizeResponse tokenResponse = tokenizerClient.tokenize(request.getCardDetails());
        log.info("Received tokenization response: {}", tokenResponse);

        Payment payment = Payment.createPayment(
                request.getMerchantReference(),
                Amount.of(request.getAmount(), request.getCurrency()),
                request.getCardDetails(),
                tokenResponse.getTokenRef()
        );
        PaymentEntity entity = PaymentEntity.fromPayment(payment);
        paymentRepository.save(entity);
        log.info("payment saved in db");

        PaymentRequestedEvent paymentRequestedEvent = PaymentRequestedEvent.newBuilder()
                .setPaymentId(payment.getPaymentId())
                .setAmount(com.zaheudev.shared.avro.Amount.newBuilder()
                        .setValue(payment.getAmount().getAmount())
                        .setCurrency(payment.getAmount().getCurrency())
                        .build())
                .setStatus(com.zaheudev.shared.avro.PaymentStatus.PENDING)
                .setTimestamp(payment.getCreatedAt().toInstant(java.time.ZoneOffset.UTC).toEpochMilli())
                .setCardRecord(com.zaheudev.shared.avro.CardRecord.newBuilder()
                        .setBin(tokenResponse.getBin())
                        .setLastFour(tokenResponse.getLastFour())
                        .setTokenRef(tokenResponse.getTokenRef())
                        .build())
                .build();

        producer.publishPaymentRequestedEvent(paymentRequestedEvent);
        log.info("payment requested event published");
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
