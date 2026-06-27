package com.zaheudev.gateway.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DlqMonitorConsumer {

    @KafkaListener(topics = {
            "payment-requests.DLT", "risk-assessed.DLT", "payment-rejected.DLT",
            "routing-completed.DLT", "authorization-completed.DLT",
            "capture-requests.DLT", "capture-completed.DLT",
            "refund-requests.DLT", "refund-completed.DLT",
            "void-requests.DLT", "void-completed.DLT"
    }, groupId = "dlq-monitor-group")
    public void consumeDlqMessage(ConsumerRecord<String, Object> record) {
        String originalTopic = record.topic().replace(".DLT", "");
        String exceptionClass = getHeaderValue(record, "Kafka_DLT_EXCEPTION_FQCN");
        String exceptionMessage = getHeaderValue(record, "Kafka_DLT_EXCEPTION_MESSAGE");

        log.error("=== DEAD LETTER MESSAGE ===");
        log.error("Original topic: {}", originalTopic);
        log.error("Payment ID (key): {}", record.key());
        log.error("Partition: {}, Offset: {}", record.partition(), record.offset());
        log.error("Exception: {}", exceptionClass);
        log.error("Error message: {}", exceptionMessage);
        log.error("===========================");
    }

    private String getHeaderValue(ConsumerRecord<String, Object> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header != null) {
            return new String(header.value());
        }
        return "N/A";
    }
}
