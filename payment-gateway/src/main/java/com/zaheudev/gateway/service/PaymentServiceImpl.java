package com.zaheudev.gateway.service;

import com.zaheudev.gateway.client.TokenizerClient;
import com.zaheudev.gateway.dto.CreatePaymentRequest;
import com.zaheudev.gateway.dto.PaymentResponse;
import com.zaheudev.gateway.dto.RefundRequest;
import com.zaheudev.gateway.entity.PaymentEntity;
import com.zaheudev.gateway.exception.PaymentFailedException;
import com.zaheudev.gateway.kafka.producer.PaymentEventProducer;
import com.zaheudev.gateway.model.Amount;
import com.zaheudev.gateway.model.Payment;
import com.zaheudev.shared.avro.CaptureRequestedEvent;
import com.zaheudev.shared.avro.RefundRequestedEvent;
import com.zaheudev.shared.dto.PaymentStatus;
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
                        .setTokenValue(tokenResponse.getTokenValue())
                        .setPrimaryNetwork(tokenResponse.getCardNetwork())
                        .setCardType(tokenResponse.getCardType())
                        .build())
                .build();

        producer.publishPaymentRequestedEvent(paymentRequestedEvent);
        log.info("payment requested event published");
        return PaymentResponse.create(payment);
    }

    @Override
    public PaymentResponse capturePayment(String paymentID) {
        PaymentEntity paymentEntity = paymentRepository.findByPaymentId(paymentID).orElseThrow(() ->
                new PaymentFailedException(null, "Payment not found with id: " + paymentID)
        );

        if(paymentEntity.getStatus() == PaymentStatus.AUTHORIZED){
            producer.publishCaptureRequestedEvent(CaptureRequestedEvent.newBuilder()
                    .setPaymentId(paymentID)
                    .build());
            log.info("The capture event for payment {} has been published", paymentID);
        }else{
            log.error("The payment {} cant be captured, it isn't authorized yet", paymentID);
            throw new PaymentFailedException(paymentEntity, "Payment is not authorized");
        }
        return paymentEntity.tranformInPaymentResponse("Request capture sent successfully");
    }

    @Override
    public PaymentResponse getPayment(String paymentId) {
        return null;
    }

    @Override
    public PaymentResponse cancelPayment(String paymentId) {
        return null;
    }

    @Override
    public PaymentResponse refundPayment(String paymentId, RefundRequest request) {
        PaymentEntity entity = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(()-> new PaymentFailedException(null,"Payment not found with id: " + paymentId));
        if(entity.getStatus() == PaymentStatus.CAPTURED || entity.getStatus() == PaymentStatus.PARTIALLY_REFUNDED){
            log.info("request: ", request.toString());
            producer.publishRefundRequestedEvent(RefundRequestedEvent.newBuilder()
                    .setPaymentId(paymentId)
                    .setRefundAmount(com.zaheudev.shared.avro.Amount.newBuilder()
                            .setValue(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                            .setCurrency(request.getCurrency())
                            .build())
                    .build());
        }else{
            log.error("The payment {} cant be refunded, it isn't captured", paymentId);
            throw new PaymentFailedException(entity, "Payment is not captured");
        }
        return null;
    }

    @Override
    public PaymentStatus getPaymentStatus(String paymentId) {
        return null;
    }
}
