package com.zaheudev.gateway.kafka.consumer;

import com.zaheudev.gateway.service.PaymentService;
import com.zaheudev.shared.avro.*;
import com.zaheudev.gateway.entity.PaymentEntity;
import com.zaheudev.gateway.exception.PaymentFailedException;
import com.zaheudev.gateway.repository.PaymentRepository;
import com.zaheudev.shared.dto.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class PaymentEventsConsumer {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @KafkaListener(topics = "authorization-completed")
    public void consumeAuthorizationCompleted(ConsumerRecord<String, AuthorizationCompletedEvent> record, Acknowledgment ack) {
        String paymentId = record.key();
        AuthorizationCompletedEvent authorizationCompletedEvent = record.value();
        log.info("Received authorization completed event for payment id: {}", paymentId);
        log.info("Authorization completed event: {}", authorizationCompletedEvent);
        if (!authorizationCompletedEvent.getSuccess()) {
            log.error("The payment has failed the authorization with error: {}", authorizationCompletedEvent.getErrorMessage());
        }
        log.info("Updating payment status for payment id: {} to {}", paymentId,
                authorizationCompletedEvent.getSuccess() ? PaymentStatus.AUTHORIZED : PaymentStatus.FAILED);
        paymentRepository.findById(paymentId).ifPresent(paymentEntity -> {
            paymentEntity.setStatus(authorizationCompletedEvent.getSuccess() ? PaymentStatus.AUTHORIZED : PaymentStatus.FAILED);
            paymentEntity.setRrn(authorizationCompletedEvent.getRrn() != null ? authorizationCompletedEvent.getRrn().toString() : null);
            paymentEntity.setAuthCode(authorizationCompletedEvent.getAuthCode() != null ? authorizationCompletedEvent.getAuthCode().toString() : null);
            paymentEntity.setProcessorTransactionId(authorizationCompletedEvent.getProcessorTransactionId() != null ? authorizationCompletedEvent.getProcessorTransactionId().toString() : null);
            paymentEntity.setErrorMessage(authorizationCompletedEvent.getErrorMessage() != null ? authorizationCompletedEvent.getErrorMessage().toString() : null);
            paymentRepository.save(paymentEntity);
            log.info("Payment status updated for payment id: {}", paymentId);
        });
        ack.acknowledge();
    }

    @KafkaListener(topics = "payment-rejected")
    public void consumePaymentRejected(ConsumerRecord<String, PaymentRejectedEvent> record, Acknowledgment ack) {
        String paymentId = record.key();
        log.error("Received payment rejected event for payment id: {}", paymentId);
        log.error("Payment rejected event due too the risk: {}", record.value());
        paymentRepository.findById(paymentId).ifPresent(paymentEntity -> {
            paymentEntity.setStatus(PaymentStatus.REJECTED);
            paymentEntity.setErrorMessage(record.value().getReason().toString());
            paymentRepository.save(paymentEntity);
            log.info("Payment status updated to REJECTED for payment id: {}", paymentId);
        });
        ack.acknowledge();
    }

    @KafkaListener(topics = "capture-completed")
    public void consumeCaptureCompleted(ConsumerRecord<String, CaptureCompletedEvent> record, Acknowledgment ack) {
        String paymentId = record.key();
        CaptureCompletedEvent captureCompletedEvent = record.value();
        log.info("Received capture completed event for payment id: {}", paymentId);
        ack.acknowledge();
        PaymentEntity entity = paymentRepository.findByPaymentId(paymentId).orElseThrow(() ->
                new PaymentFailedException(null, "Payment doesnt exist"));
        entity.setStatus(captureCompletedEvent.getSuccess() ? PaymentStatus.CAPTURED : PaymentStatus.FAILED);
        entity.setCaptureId(captureCompletedEvent.getCaptureId().toString());
        paymentRepository.save(entity);
        log.info("Payment status updated for payment id: {} to {}", paymentId, entity.getStatus());
    }

    @KafkaListener(topics = "refund-completed")
    public void consumeRefundCompleted(ConsumerRecord<String, RefundCompletedEvent> record, Acknowledgment ack) {
        String paymentId = record.key();
        RefundCompletedEvent refundCompletedEvent = record.value();
        log.info("Received refund completed event for payment id: {}", paymentId);
        if (!refundCompletedEvent.getSuccess()) {
            log.error("Refund status failed for payment: {} due to error: {}", paymentId, refundCompletedEvent.getErrorMessage());
            log.error("Stoping the refund process for payment id: {}", paymentId);
            ack.acknowledge();
        } else {
            ack.acknowledge();
            PaymentEntity entity = paymentRepository.findByPaymentId(paymentId).orElseThrow(() ->
                    new PaymentFailedException(null, "Payment doesnt exist"));
            entity.setStatus(refundCompletedEvent.getStatus().toString().equals("REFUNDED") ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
            entity.setRefundedAmount(BigDecimal.valueOf(refundCompletedEvent.getRefundedAmount().getValue()).divide(BigDecimal.valueOf(100)));
            paymentRepository.save(entity);
            log.info("Refunded processed sucessfully status updated to for payment id: {} to {}", paymentId, entity.getStatus());
        }
    }

    @KafkaListener(topics = "void-completed")
    public void consumeVoidCompleted(ConsumerRecord<String, VoidCompletedEvent> record, Acknowledgment ack) {
        String paymentId = record.key();
        VoidCompletedEvent voidCompletedEvent = record.value();
        log.info("Received void completed event for payment id: {}", paymentId);
        if (!voidCompletedEvent.getSuccess()) {
            log.error("Void status failed for payment: {} due to error: {}", paymentId, voidCompletedEvent.getErrorMessage());
            log.error("Stoping the void process for payment id: {}", paymentId);
            ack.acknowledge();
        } else {
            ack.acknowledge();
            PaymentEntity entity = paymentRepository.findByPaymentId(paymentId).orElseThrow(() ->
                    new PaymentFailedException(null, "Payment doesnt exist"));
            entity.setStatus(PaymentStatus.VOID);
            paymentRepository.save(entity);
            log.info("Void processed sucessfully status updated to for payment id: {} to {}", paymentId, entity.getStatus());
        }
    }

    @KafkaListener(topics = "risk-assessed")
    public void consumeRiskAssessed(ConsumerRecord<String, RiskAssessedEvent> record, Acknowledgment ack){
        String paymentId = record.key();
        RiskAssessedEvent riskAssessedEvent = record.value();
        log.info("Received risk assessed event for payment id: {}", paymentId);
        log.info("Risk assessed event: {}", riskAssessedEvent);
        ack.acknowledge();
        try{
            paymentService.updatePaymentStatus(paymentId, PaymentStatus.RISK_ASSESSED, PaymentStatus.CREATED);
        }catch(PaymentFailedException e){
            log.error("Error updating payment status for payment id: {}, cause: {}", paymentId, e.getMessage());
        }
        ack.acknowledge();
    }

    @KafkaListener(topics = "routing-completed")
    public void consumeRoutingCompleted(ConsumerRecord<String, RoutedCompletedEvent> record, Acknowledgment ack) {
        String paymentId = record.key();
        RoutedCompletedEvent routedCompletedEvent = record.value();
        log.info("Received routing completed event for payment id: {}", paymentId);
        log.info("Estimated cost: {}, in network: {}", routedCompletedEvent.getEstimatedCost(), routedCompletedEvent.getSelectedPaymentMethod());
        ack.acknowledge();
        try {
            paymentService.updatePaymentStatus(paymentId, PaymentStatus.ROUTING_COMPLETED, PaymentStatus.RISK_ASSESSED);
            paymentService.updateNetworkFee(paymentId, new BigDecimal(routedCompletedEvent.getEstimatedCost().toString()));
        } catch (PaymentFailedException e) {
            log.error("Error updating payment status for payment id: {}, cause: {}", paymentId, e.getMessage());
        }
        ack.acknowledge();
    }

}
