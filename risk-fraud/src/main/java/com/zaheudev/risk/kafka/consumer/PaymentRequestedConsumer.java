package com.zaheudev.risk.kafka.consumer;

import com.zaheudev.risk.entity.RiskAssessmentEntity;
import com.zaheudev.risk.kafka.producer.RiskAssessProducer;
import com.zaheudev.risk.model.RiskLevel;
import com.zaheudev.risk.repository.RiskAssessmentRepository;
import com.zaheudev.risk.service.RiskService;
import com.zaheudev.shared.avro.PaymentRequestedEvent;
import com.zaheudev.shared.avro.RiskAssessed;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class PaymentRequestedConsumer {
    @Autowired
    private RiskService riskService;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private RiskAssessProducer riskAssessProducer;

    @KafkaListener(topics = "payment-requests")
    public void consume(ConsumerRecord<String, PaymentRequestedEvent> record, Acknowledgment ack){
        String paymentId = record.key();
        PaymentRequestedEvent event  = record.value();
        log.info("Calculating the risk for the payment with id: {}", paymentId);
        try{
            RiskLevel riskLevel = riskService.assessRisk(paymentId);
            RiskAssessmentEntity riskEntity = RiskAssessmentEntity.builder()
                    .assessmentId(UUID.randomUUID().toString())
                    .paymentId(paymentId)
                    .riskLevel(riskLevel)
                    .riskReason("Risk calculated based on transaction history and card details")
                    .approved(riskLevel == RiskLevel.MEDIUM)
                    .assessmentDate(java.time.LocalDate.now())
                    .build();
            riskAssessmentRepository.save(riskEntity);
            log.info("Risk assessment saved in db");
            ack.acknowledge();
            log.info("Publishing risk to kafka");
            riskAssessProducer.publishRiskAssessmentEvent(RiskAssessed.newBuilder()
                    .setAssessmentId(riskEntity.getAssessmentId())
                    .setPaymentId(paymentId)
                    .setRiskLevel(com.zaheudev.shared.avro.RiskLevel.valueOf(riskLevel.name()))
                    .setReason(riskEntity.getRiskReason())
                    .setApproved(riskEntity.isApproved())
                    .setTimestamp(System.currentTimeMillis())
                    .setCardRecord(event.getCardRecord())
                    .setAmount(event.getAmount())
                    .build());
        }catch(Exception e){
            log.error("Error while calculating risk for payment: {}", paymentId);
            log.error("Error: {}", e.getMessage());
        }
    }
}
