package com.zaheudev.bff.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DltController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @GetMapping("/api/dlt/topics")
    public List<String> getDltTopics() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (AdminClient admin = AdminClient.create(props)) {
            return admin.listTopics().names().get().stream()
                    .filter(t -> t.endsWith(".DLT"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list DLT topics", e);
            return List.of();
        }
    }

    @GetMapping("/api/dlt/{topic}")
    public List<Map<String, Object>> getDltMessages(@PathVariable String topic,
                                                     @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> messages = new ArrayList<>();
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-browser-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, limit);

        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(p -> new TopicPartition(topic, p.partition()))
                    .collect(Collectors.toList());
            consumer.assign(partitions);
            consumer.seekToEnd(partitions);

            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
            for (TopicPartition tp : partitions) {
                long start = Math.max(0, endOffsets.get(tp) - limit);
                consumer.seek(tp, start);
            }

            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(3));
            for (ConsumerRecord<String, byte[]> record : records) {
                Map<String, Object> msg = new LinkedHashMap<>();
                msg.put("offset", record.offset());
                msg.put("partition", record.partition());
                msg.put("key", record.key());
                msg.put("timestamp", record.timestamp());
                msg.put("value", record.value() != null ? new String(record.value()).substring(0, Math.min(200, new String(record.value()).length())) : null);
                messages.add(msg);
            }
        }
        return messages;
    }

    @PostMapping("/api/dlt/{topic}/replay/{partition}/{offset}")
    public ResponseEntity<Map<String, Object>> replayMessage(
            @PathVariable String topic,
            @PathVariable int partition,
            @PathVariable long offset) {

        String originTopic = topic.replace(".DLT", "");

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-replayer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            TopicPartition tp = new TopicPartition(topic, partition);
            consumer.assign(List.of(tp));
            consumer.seek(tp, offset);
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(3));
            for (ConsumerRecord<String, byte[]> record : records) {
                if (record.offset() == offset) {
                    kafkaTemplate.send(originTopic, record.key(), record.value());
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("status", "replayed");
                    result.put("sourceTopic", topic);
                    result.put("targetTopic", originTopic);
                    result.put("offset", offset);
                    return ResponseEntity.ok(result);
                }
            }
        } catch (Exception e) {
            log.error("Failed to replay message", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.notFound().build();
    }
}
