package com.zaheudev.gateway.kafka.consumer;

import com.zaheudev.gateway.repository.PaymentRepository;
import com.zaheudev.gateway.service.PaymentServiceImpl;
import com.zaheudev.shared.avro.AuthorizationCompleted;
import com.zaheudev.shared.dto.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthorizationCompletedConsumer {
    @Autowired
    private PaymentRepository paymentRepository;

    @KafkaListener(topics = "authorization-completed")
    public void consume(ConsumerRecord<String, AuthorizationCompleted> record, Acknowledgment ack) {
        String paymentId = record.key();
        AuthorizationCompleted authorizationCompleted = record.value();
        log.info("Received authorization completed event for payment id: {}", paymentId);
        log.info("Authorization completed event: {}", authorizationCompleted);
        if(!authorizationCompleted.getSuccess()){
            log.error("The payment has failed the authorization with error: {}", authorizationCompleted.getErrorMessage());
        }
        log.info("Updating payment status for payment id: {} to {}", paymentId,
                authorizationCompleted.getSuccess() ?  PaymentStatus.AUTHORIZED: PaymentStatus.FAILED);
        paymentRepository.findById(paymentId).ifPresent(paymentEntity -> {
            paymentEntity.setStatus(authorizationCompleted.getSuccess() ? PaymentStatus.AUTHORIZED : PaymentStatus.FAILED);
            paymentEntity.setRrn(authorizationCompleted.getRrn().toString());
            paymentEntity.setAuthCode(authorizationCompleted.getAuthCode().toString());
            paymentEntity.setProcessorTransactionId(authorizationCompleted.getProcessorTransactionId().toString());
            paymentEntity.setErrorMessage(authorizationCompleted.getErrorMessage() != null ? authorizationCompleted.getErrorMessage().toString() : null);
            paymentRepository.save(paymentEntity);
            log.info("Payment status updated for payment id: {}", paymentId);
        });
        ack.acknowledge();
    }
}
