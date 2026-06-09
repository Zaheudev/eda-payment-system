package com.zaheudev.integration.metrics;

import com.zaheudev.integration.EdaInfrastructure;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventRecorder {

    private static final Logger log = LoggerFactory.getLogger(EventRecorder.class);

    private static final List<String> ALL_TOPICS = List.of(
            "payment-requests", "risk-assessed", "payment-rejected",
            "routing-completed", "authorization-completed",
            "capture-requests", "capture-completed",
            "refund-requests", "refund-completed",
            "void-requests", "void-completed"
    );

    private final KafkaConsumer<String, GenericRecord> consumer;
    private final ConcurrentHashMap<String, List<RecordedEvent>> eventsByPaymentId = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<RecordedEvent> allEvents = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread pollThread;
    private Instant startTimestamp;

    public EventRecorder() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, EdaInfrastructure.getKafkaBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "metrics-recorder-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("schema.registry.url", EdaInfrastructure.getSchemaRegistryUrl());
        props.put("specific.avro.reader", "true");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(ALL_TOPICS);

        startTimestamp = Instant.now();

        pollThread = new Thread(this::pollLoop, "event-recorder");
        pollThread.setDaemon(true);
        pollThread.start();

        log.info("EventRecorder started, listening on {} topics", ALL_TOPICS.size());
    }

    public void resetStartTimestamp() {
        startTimestamp = Instant.now();
        eventsByPaymentId.clear();
        allEvents.clear();
    }

    private void pollLoop() {
        while (running.get()) {
            ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, GenericRecord> record : records) {
                var evt = new RecordedEvent(
                        record.topic(),
                        record.key(),
                        record.value(),
                        Instant.ofEpochMilli(record.timestamp())
                );
                allEvents.add(evt);
                eventsByPaymentId
                        .computeIfAbsent(record.key(), k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(evt);
            }
        }
    }

    public List<RecordedEvent> getEventsForPayment(String paymentId) {
        return eventsByPaymentId.getOrDefault(paymentId, List.of());
    }

    public List<RecordedEvent> getAllEvents() {
        return new ArrayList<>(allEvents);
    }

    public long countEventsOnTopic(String topic) {
        return allEvents.stream()
                .filter(e -> e.topic().equals(topic))
                .filter(e -> e.timestamp().isAfter(startTimestamp) || e.timestamp().equals(startTimestamp))
                .count();
    }

    public Optional<Long> latencyBetween(String paymentId, String fromTopic, String toTopic) {
        var from = eventsByPaymentId.getOrDefault(paymentId, List.of()).stream()
                .filter(e -> e.topic().equals(fromTopic)).map(RecordedEvent::timestamp)
                .min(Instant::compareTo);
        var to = eventsByPaymentId.getOrDefault(paymentId, List.of()).stream()
                .filter(e -> e.topic().equals(toTopic)).map(RecordedEvent::timestamp)
                .min(Instant::compareTo);
        if (from.isPresent() && to.isPresent()) {
            return Optional.of(to.get().toEpochMilli() - from.get().toEpochMilli());
        }
        return Optional.empty();
    }

    public void stop() {
        running.set(false);
        try {
            pollThread.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        consumer.close();
        log.info("EventRecorder stopped. Recorded {} total events.", allEvents.size());
    }

    public record RecordedEvent(String topic, String key, GenericRecord value, Instant timestamp) {
        public String getPaymentId() { return key(); }
    }
}
