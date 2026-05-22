package com.zaheudev.emulator.kafka.consumer;

import com.zaheudev.emulator.entity.EmulatedRefundTransactionEntity;
import com.zaheudev.emulator.repository.EmulatedRefundTransactionRepository;
import com.zaheudev.shared.avro.*;
import com.zaheudev.emulator.entity.EmulatedTransactionEntity;
import com.zaheudev.emulator.kafka.producer.EmulatorProcessorProducer;
import com.zaheudev.emulator.model.TransactionStatus;
import com.zaheudev.emulator.repository.EmulatedTransactionRepository;
import com.zaheudev.emulator.service.EmulatedCardProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class EmulatorProcessorConsumer {
    @Autowired
    private EmulatedCardProcessor cardProcessor;

    @Autowired
    private EmulatedTransactionRepository transactionRepository;

    @Autowired
    private EmulatedRefundTransactionRepository refundTransactionRepository;
    
    @Autowired
    private EmulatorProcessorProducer kafkaProducer;

    @KafkaListener(topics = "routing-completed")
    public void consumeRoutedCompleted(ConsumerRecord<String, RoutedCompletedEvent> record, Acknowledgment ack){
        log.info("Received routing completed event for payment id: {}", record.key());
        String paymentId = record.key();
        try{

        if(transactionRepository.existsByPaymentId(paymentId)){
            log.error("Payment with id {} already exists in the database. Skipping processing.", paymentId);
            ack.acknowledge();
            return;
        }
        RoutedCompletedEvent event  = record.value();
        AuthorizationCompletedEvent authCompleted = cardProcessor.authorize(
                paymentId,
                event.getCardRecord(),
                event.getSelectedPaymentMethod(),
                BigDecimal.valueOf(event.getAmount().getValue()),
                event.getAmount().getCurrency().toString()
        );
        log.info("Authorization result for payment id {} saved in database", paymentId);
        ack.acknowledge();
        transactionRepository.save(EmulatedTransactionEntity.builder()
                .paymentId(paymentId)
                .processorTransactionId(authCompleted.getProcessorTransactionId().toString())
                .rrn(authCompleted.getRrn().toString())
                .authCode(authCompleted.getAuthCode().toString())
                .selectedPaymentMethod(authCompleted.getSelectedPaymentMethod())
                .networkFee(BigDecimal.valueOf(Double.parseDouble(event.getEstimatedCost().toString())))
                .authorizedAmount(BigDecimal.valueOf(event.getAmount().getValue()))
                .currency(event.getAmount().getCurrency().toString())
                .transactionStatus(authCompleted.getSuccess() ? TransactionStatus.AUTHORIZED : TransactionStatus.FAILED)
                .errorMessage(authCompleted.getErrorMessage()  != null ? authCompleted.getErrorMessage().toString() : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        log.info("Persiting authorization result for : {}", authCompleted.getProcessorTransactionId().toString());
        ack.acknowledge();
        
        kafkaProducer.publishAuthorizationCompleteEvent(authCompleted);
        log.info("Published to kafka authorization completed event: {}", authCompleted);
        }catch (Exception e){
            log.error("Error processing routing completed event for payment id {}: {}", paymentId, e.getMessage());
        }
    }

    @KafkaListener(topics = "capture-requests")
    public void consumeCaptureRequested(ConsumerRecord<String, CaptureRequestedEvent> record, Acknowledgment ack){
        log.info("Received capture requested event for payment id: {}", record.key());
        String paymentId = record.key();
        EmulatedTransactionEntity entity = transactionRepository.findByPaymentId(paymentId).orElseThrow(()-> {
            log.error("Payment with id {} not found in the database. Skipping processing.", paymentId);
            return new RuntimeException("Payment not found with id: " + paymentId);
        });
        if(entity.getTransactionStatus() != TransactionStatus.AUTHORIZED){
            log.error("Payment with id {} is not authorized. Current status: {}. Skipping processing.", paymentId, entity.getTransactionStatus());
            ack.acknowledge();
            return;
        }
        CaptureCompletedEvent captureResult = cardProcessor.capture(entity);
        if(!captureResult.getSuccess()){
            log.error("Capture refused by issuer, payment id: {}", paymentId);
            log.error("continuing processing....");
        }
        entity.setTransactionStatus(captureResult.getSuccess() ? TransactionStatus.CAPTURED : TransactionStatus.FAILED);
        entity.setCaptureId(captureResult.getCaptureId().toString());
        entity.setCapturedAmount(BigDecimal.valueOf(captureResult.getAmount().getValue()));
        entity.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(entity);
        log.info("Updated transaction status for payment id {} to {}", paymentId, entity.getTransactionStatus());
        ack.acknowledge();
        kafkaProducer.publishCaptureCompletedEvent(captureResult);
        log.info("Published to kafka capture completed event: {}", captureResult);
        ack.acknowledge();
    }

    @KafkaListener(topics = "refund-requests")
    public void consumeRefundRequested(ConsumerRecord<String, RefundRequestedEvent> record, Acknowledgment ack){
        log.info("Received refund requested event for payment id: {}", record.key());
        String paymentId = record.key();
        RefundRequestedEvent event = record.value();
        TransactionStatus state = TransactionStatus.REFUNDED;
        EmulatedTransactionEntity transactionEntity = transactionRepository.findByPaymentId(paymentId).orElseThrow(()-> {
            log.error("Payment with id {} not found in the database. Skipping processing.", paymentId);
            return new RuntimeException("Payment not found with id: " + paymentId);
        });
        if(transactionEntity.getTransactionStatus() != TransactionStatus.CAPTURED && transactionEntity.getTransactionStatus() != TransactionStatus.PARTIALLY_REFUNDED){
            log.error("Payment with id {} is not captured. Current status: {}. Skipping processing.", paymentId, transactionEntity.getTransactionStatus());
            ack.acknowledge();
            return;
        }
        List<EmulatedRefundTransactionEntity> refunds = refundTransactionRepository.findByPaymentId(paymentId);
        log.info("Event: {}", event);
        BigDecimal refundAmount = BigDecimal.valueOf(
                Double.parseDouble(event.getRefundAmount() != null ?
                        event.getRefundAmount().toString() : transactionEntity.getCapturedAmount().toString()));

        BigDecimal totalRefundedAmount = refunds.stream()
                    .filter(refund -> refund.getTransactionStatus() == TransactionStatus.REFUNDED)
                    .map(EmulatedRefundTransactionEntity::getRefundedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

        if(totalRefundedAmount.add(refundAmount).compareTo(transactionEntity.getCapturedAmount()) > 0){
            log.error("Total refunded amount {} exceeds captured amount {} for payment id {}. Skipping processing.",
                    totalRefundedAmount, transactionEntity.getCapturedAmount(), paymentId);
            ack.acknowledge();
            return;
        }else if(totalRefundedAmount.add(refundAmount).compareTo(transactionEntity.getCapturedAmount()) < 0){
            log.info("Total refunded amount {} is less than captured amount {} for payment id {}. Setting state to PARTIALLY_REFUNDED",
                    totalRefundedAmount.add(refundAmount), transactionEntity.getCapturedAmount(), paymentId);
            state = TransactionStatus.PARTIALLY_REFUNDED;
            ack.acknowledge();
        }
        RefundCompletedEvent refundResult = cardProcessor.refund(paymentId, transactionEntity.getProcessorTransactionId(),
                refundAmount, transactionEntity.getCurrency());
        if(!refundResult.getSuccess()){
            EmulatedRefundTransactionEntity refundEntity = EmulatedRefundTransactionEntity.builder()
                    .paymentId(paymentId)
                    .refundId(refundResult.getRefundId().toString())
                    .originalProcessorTransactionId(transactionEntity)
                    .refundedAmount(refundAmount)
                    .refundRrn(refundResult.getRefundRrn().toString())
                    .refundAuthCode(refundResult.getRefundAuthCode().toString())
                    .currency(transactionEntity.getCurrency())
                    .transactionStatus(TransactionStatus.FAILED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .errorMessage("Refund failed, refused by issuer. Please try again later.")
                    .build();
            refundTransactionRepository.save(refundEntity);
            ack.acknowledge();
            return;
        }
        transactionEntity.setTransactionStatus(state);
        transactionEntity.setUpdatedAt(LocalDateTime.now());
        transactionEntity.setRefundedAmount(totalRefundedAmount.add(refundAmount));
        transactionRepository.save(transactionEntity);
        ack.acknowledge();
        log.info("Updated transaction status for payment id {} to {} and persisting it.", paymentId, transactionEntity.getTransactionStatus());
        EmulatedRefundTransactionEntity refundEntity = EmulatedRefundTransactionEntity.builder()
                .paymentId(paymentId)
                .refundId(refundResult.getRefundId().toString())
                .originalProcessorTransactionId(transactionEntity)
                .refundedAmount(refundAmount)
                .refundRrn(refundResult.getRefundRrn().toString())
                .refundAuthCode(refundResult.getRefundAuthCode().toString())
                .currency(transactionEntity.getCurrency())
                .transactionStatus(TransactionStatus.REFUNDED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        refundTransactionRepository.save(refundEntity);
        ack.acknowledge();
        log.info("Persisted refund transaction for payment id {}: {}", paymentId, refundEntity);
        log.info("Publishing to kafka...");
        refundResult.setStatus(state.toString());
        kafkaProducer.publishRefundCompletedEvent(refundResult);
        ack.acknowledge();
    }
}
