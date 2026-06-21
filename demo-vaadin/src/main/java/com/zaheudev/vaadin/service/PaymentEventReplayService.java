package com.zaheudev.vaadin.service;

import com.zaheudev.vaadin.model.EventEnvelope;
import com.zaheudev.vaadin.util.AvroToMap;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PaymentEventReplayService {

    private static final List<String> TOPICS = List.of(
            "payment-requests", "risk-assessed", "payment-rejected",
            "routing-completed", "authorization-completed",
            "capture-requests", "capture-completed",
            "refund-requests", "refund-completed",
            "void-requests", "void-completed"
    );

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);
    private static final int MAX_EMPTY_POLLS = 3;

    private final String bootstrapServers;
    private final String schemaRegistryUrl;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "payment-replay");
        t.setDaemon(true);
        return t;
    });

    public PaymentEventReplayService(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.properties.schema.registry.url}") String schemaRegistryUrl) {
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    public CompletableFuture<List<EventEnvelope>> replayForPayment(String paymentId) {
        return CompletableFuture.supplyAsync(() -> doReplay(paymentId), executor);
    }

    private List<EventEnvelope> doReplay(String paymentId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "replay-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", false);

        List<EventEnvelope> result = new ArrayList<>();

        try (KafkaConsumer<String, GenericRecord> consumer = new KafkaConsumer<>(props)) {
            Collection<TopicPartition> partitions = new ArrayList<>();
            for (String topic : TOPICS) {
                for (org.apache.kafka.common.PartitionInfo pi : consumer.partitionsFor(topic)) {
                    partitions.add(new TopicPartition(topic, pi.partition()));
                }
            }
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

            int emptyPolls = 0;
            while (!allReachedEnd(consumer, endOffsets) && emptyPolls < MAX_EMPTY_POLLS) {
                ConsumerRecords<String, GenericRecord> records = consumer.poll(POLL_TIMEOUT);
                if (records.isEmpty()) {
                    emptyPolls++;
                    continue;
                }
                emptyPolls = 0;
                for (ConsumerRecord<String, GenericRecord> record : records) {
                    String key = record.key();
                    if (key == null || !key.equals(paymentId)) {
                        Object pidFromValue = extractPaymentId(record.value());
                        if (pidFromValue == null || !pidFromValue.equals(paymentId)) continue;
                    }
                    result.add(toEnvelope(record));
                }
            }
        } catch (Exception e) {
            log.warn("Replay failed for payment {}: {}", paymentId, e.getMessage());
        }

        result.sort(java.util.Comparator.comparingLong(EventEnvelope::getTimestamp));
        return result;
    }

    private boolean allReachedEnd(KafkaConsumer<String, GenericRecord> consumer, Map<TopicPartition, Long> endOffsets) {
        for (Map.Entry<TopicPartition, Long> e : endOffsets.entrySet()) {
            if (consumer.position(e.getKey()) < e.getValue()) return false;
        }
        return true;
    }

    private EventEnvelope toEnvelope(ConsumerRecord<String, GenericRecord> record) {
        return EventEnvelope.builder()
                .topic(record.topic())
                .key(record.key())
                .timestamp(record.timestamp())
                .payload(AvroToMap.convert(record.value()))
                .paymentId(record.key())
                .partition(record.partition())
                .offset(record.offset())
                .build();
    }

    private String extractPaymentId(Object value) {
        if (value instanceof GenericRecord gr && gr.hasField("paymentId")) {
            Object pid = gr.get("paymentId");
            if (pid instanceof CharSequence cs) return cs.toString();
            if (pid instanceof ByteBuffer bb) {
                byte[] bytes = new byte[bb.remaining()];
                bb.duplicate().get(bytes);
                return new String(bytes);
            }
        }
        return null;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
