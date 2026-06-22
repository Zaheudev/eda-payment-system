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
import com.zaheudev.shared.avro.VoidRequestedEvent;
import com.zaheudev.shared.dto.PaymentStatus;
import com.zaheudev.gateway.repository.PaymentRepository;
import com.zaheudev.shared.avro.PaymentRequestedEvent;
import com.zaheudev.shared.dto.TokenizeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventProducer producer;

    @Autowired
    private TokenizerClient tokenizerClient;

    public PaymentResponse createPayment(CreatePaymentRequest request) {
        TokenizeResponse tokenResponse = tokenizerClient.tokenize(request.getCardDetails());
        log.info("Received tokenization response: {}", tokenResponse);

        Payment payment = Payment.createPayment(
                request.getMerchantRef(),
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
                .setTimestamp(payment.getCreatedAt().toInstant(java.time.ZoneOffset.UTC).toEpochMilli())
                .setMerchantRef(payment.getMerchantRef())
                .setCardRecord(com.zaheudev.shared.avro.CardRecord.newBuilder()
                        .setBin(tokenResponse.getBin())
                        .setLastFour(tokenResponse.getLastFour())
                        .setTokenRef(tokenResponse.getTokenRef())
                        .setTokenValue(tokenResponse.getTokenValue())
                        .setPrimaryNetwork(tokenResponse.getCardNetwork())
                        .setCardType(tokenResponse.getCardType())
                        .setTokenStatus(tokenResponse.getStatus().name())
                        .build())
                .build();

        producer.publishPaymentRequestedEvent(paymentRequestedEvent);
        log.info("payment requested event published");
        return PaymentResponse.create(payment);
    }

    public PaymentResponse capturePayment(String paymentId) {
        PaymentEntity paymentEntity = getPaymentEntity(paymentId);

        if(paymentEntity.getStatus() == PaymentStatus.AUTHORIZED){
            producer.publishCaptureRequestedEvent(CaptureRequestedEvent.newBuilder()
                    .setPaymentId(paymentId)
                    .build());
            log.info("The capture event for payment {} has been published", paymentId);
        }else{
            log.error("The payment {} cant be captured, it isn't authorized yet", paymentId);
            throw new PaymentFailedException(paymentEntity, "Payment is not authorized");
        }
        return paymentEntity.tranformInPaymentResponse("Request capture sent successfully");
    }

    public PaymentResponse voidPayment(String paymentId) {
        PaymentEntity paymentEntity = getPaymentEntity(paymentId);
        if(paymentEntity.getStatus() == PaymentStatus.AUTHORIZED){
            producer.publishVoidRequestedEvent(VoidRequestedEvent.newBuilder()
                    .setPaymentId(paymentId)
                    .build());
            log.info("The payment {} has been voided successfully", paymentId);
        }else{
            log.error("The payment {} cant be voided, it isn't authorized yet", paymentId);
            throw new PaymentFailedException(paymentEntity, "Payment is not authorized");
        }
        return paymentEntity.tranformInPaymentResponse("Request void sent successfully");
    }

    public PaymentResponse refundPayment(String paymentId, RefundRequest request) {
        PaymentEntity entity = getPaymentEntity(paymentId);
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

    public void updatePaymentStatus(String paymentId, PaymentStatus status, PaymentStatus previousStatus) {
        PaymentEntity payment = getPaymentEntity(paymentId);

        PaymentStatus current = payment.getStatus();

        if (isAtOrPast(current, status)) {
            log.debug("Payment {} already at {} (target was {}), skipping idempotent update",
                    paymentId, current, status);
            return;
        }

        if (current == previousStatus || current == PaymentStatus.PENDING) {
            payment.setStatus(status);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            return;
        }

        throw new PaymentFailedException(payment,
                "Invalid transition: cannot go from " + current + " to " + status
                        + " (expected predecessor " + previousStatus + ")");
    }

    private boolean isAtOrPast(PaymentStatus current, PaymentStatus target) {
        return stateOrder(current) >= stateOrder(target);
    }

    private int stateOrder(PaymentStatus status) {
        return switch (status) {
            case CREATED, PENDING -> 0;
            case RISK_ASSESSED -> 1;
            case ROUTING_COMPLETED -> 2;
            case AUTHORIZED -> 3;
            case CAPTURED, PARTIALLY_CAPTURED, VOID, FAILED -> 4;
            case REFUNDED, PARTIALLY_REFUNDED -> 5;
            case REJECTED -> -2;
            default -> -1;
        };
    }

    public ResponseEntity<PaymentResponse> getResponseEntity(int code, PaymentEntity response){
        if(response == null){
            return ResponseEntity.status(404).body(null);
        }
        return ResponseEntity.status(code).body(PaymentResponse.builder()
                .paymentId(response.getPaymentId())
                .rrn(response.getRrn())
                .authCode(response.getAuthCode())
                .processorTransactionId(response.getProcessorTransactionId())
                .message(response.getErrorMessage())
                .paymentStatus(response.getStatus())
                .createdAt(response.getCreatedAt())
                .message(response.getErrorMessage())
                .amount(Amount.builder()
                        .amount(response.getAmount().longValue())
                        .currency(response.getCurrency())
                        .build())
                .build());
    }

    public void updateNetworkFee(String paymentId, BigDecimal networkFee){
        PaymentEntity payment = getPaymentEntity(paymentId);
        payment.setNetworkFee(networkFee);
        paymentRepository.save(payment);
    }

    public PaymentEntity getPaymentEntity(String paymentId){
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentFailedException(null, "Payment not found with id: " + paymentId));
    }

    public PaymentStatus getPaymentStatus(String paymentId) {
        return null;
    }

    public PaymentResponse getPayment(String paymentId) {
        PaymentEntity paymentEntity = getPaymentEntity(paymentId);
        return paymentEntity.tranformInPaymentResponse(null);
    }

    public List<PaymentResponse> getAllPayments(int limit) {
        int safe = Math.max(1, Math.min(limit, 100));
        return paymentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safe))
                .stream()
                .map(e -> e.tranformInPaymentResponse(null))
                .toList();
    }
}
