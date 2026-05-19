package com.zaheudev.emulator.kafka.consumer;

import com.zaheudev.emulator.entity.EmulatedTransactionEntity;
import com.zaheudev.emulator.kafka.producer.AuthorizationCompletedProducer;
import com.zaheudev.emulator.model.TransactionStatus;
import com.zaheudev.emulator.repository.EmulatedTransactionRepository;
import com.zaheudev.emulator.service.EmulatedCardProcessor;
import com.zaheudev.shared.avro.AuthorizationCompleted;
import com.zaheudev.shared.avro.RoutedCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
public class RoutedCompletedConsumer {
    @Autowired
    private EmulatedCardProcessor cardProcessor;

    @Autowired
    private EmulatedTransactionRepository repository;
    
    @Autowired
    private AuthorizationCompletedProducer authCompletedProducer;

    @KafkaListener(topics = "routing-completed")
    public void consume(ConsumerRecord<String, RoutedCompletedEvent> record, Acknowledgment ack){
        log.info("Received routing completed event for payment id: {}", record.key());
        String paymentId = record.key();
        if(repository.existsByPaymentId(paymentId)){
            log.error("Payment with id {} already exists in the database. Skipping processing.", paymentId);
            ack.acknowledge();
            return;
        }
        RoutedCompletedEvent event  = record.value();
        AuthorizationCompleted authCompleted = cardProcessor.authorize(
                paymentId,
                event.getCardRecord(),
                event.getSelectedPaymentMethod(),
                BigDecimal.valueOf(event.getAmount().getValue()),
                event.getAmount().getCurrency().toString()
        );
        log.info("Authorization result for payment id {} saved in database", paymentId);
        ack.acknowledge();
        repository.save(EmulatedTransactionEntity.builder()
                .paymentId(paymentId)
                .processorTransactionId(authCompleted.getProcessorTransactionId().toString())
                .rrn(authCompleted.getRrn().toString())
                .authCode(authCompleted.getAuthCode().toString())
                .selectedPaymentMethod(authCompleted.getSelectedPaymentMethod())
                .authorizedAmount(BigDecimal.valueOf(event.getAmount().getValue()))
                .currency(event.getAmount().getCurrency().toString())
                .transactionStatus(authCompleted.getSuccess() ? TransactionStatus.AUTHORIZED : TransactionStatus.FAILED)
                .errorMessage(authCompleted.getErrorMessage()  != null ? authCompleted.getErrorMessage().toString() : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        log.info("Persiting authorization result for : {}", authCompleted.getProcessorTransactionId().toString());
        ack.acknowledge();
        
        authCompletedProducer.publishAuthorizationCompleteEvent(authCompleted);
        log.info("Published to kafka authorization completed event: {}", authCompleted);        
    }
}
