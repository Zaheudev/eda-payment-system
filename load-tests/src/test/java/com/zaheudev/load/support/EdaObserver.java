package com.zaheudev.load.support;

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
import java.util.concurrent.atomic.AtomicLong;

public class EdaObserver {

    private static final Logger log = LoggerFactory.getLogger(EdaObserver.class);

    private static final List<String> ALL_TOPICS = List.of(
            "payment-requests", "risk-assessed", "payment-rejected",
            "routing-completed", "authorization-completed",
            "capture-requests", "capture-completed",
            "refund-requests", "refund-completed",
            "void-requests", "void-completed"
    );

    private final KafkaConsumer<String, GenericRecord> consumer;
    private final ConcurrentHashMap<String, List<ObservedEvent>> eventsByPaymentId = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ObservedEvent> allEvents = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread pollThread;
    private Instant observationStart;

    public EdaObserver() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, LoadConfig.kafkaBootstrap());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "eda-load-observer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("schema.registry.url", LoadConfig.schemaRegistryUrl());
        props.put("specific.avro.reader", "true");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(ALL_TOPICS);

        pollThread = new Thread(this::pollLoop, "eda-observer");
        pollThread.setDaemon(true);
    }

    public void start() {
        observationStart = Instant.now();
        pollThread.start();
        log.info("EdaObserver started, listening on {} topics", ALL_TOPICS.size());
    }

    public void stop() {
        running.set(false);
        try {
            pollThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        consumer.close();
        log.info("EdaObserver stopped. Recorded {} total events.", allEvents.size());
    }

    private void pollLoop() {
        while (running.get()) {
            ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, GenericRecord> record : records) {
                var evt = new ObservedEvent(record.topic(), record.key(), record.value(),
                        Instant.ofEpochMilli(record.timestamp()));
                allEvents.add(evt);
                eventsByPaymentId
                        .computeIfAbsent(record.key(), k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(evt);
            }
        }
    }

    public long ingestionCount() {
        return allEvents.stream()
                .filter(e -> "payment-requests".equals(e.topic))
                .count();
    }

    public long completionCount() {
        return allEvents.stream()
                .filter(e -> "authorization-completed".equals(e.topic))
                .count();
    }

    public Set<String> ingestedPaymentIds() {
        Set<String> ids = new HashSet<>();
        for (var e : allEvents) {
            if ("payment-requests".equals(e.topic)) {
                ids.add(e.key);
            }
        }
        return ids;
    }

    public Set<String> completedPaymentIds() {
        Set<String> ids = new HashSet<>();
        for (var e : allEvents) {
            if ("authorization-completed".equals(e.topic)) {
                ids.add(e.key);
            }
        }
        return ids;
    }

    public List<Long> e2eLatencies() {
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        for (var entry : eventsByPaymentId.entrySet()) {
            var events = entry.getValue();
            var from = events.stream()
                    .filter(e -> "payment-requests".equals(e.topic))
                    .map(ObservedEvent::timestamp)
                    .min(Instant::compareTo);
            var to = events.stream()
                    .filter(e -> "authorization-completed".equals(e.topic))
                    .map(ObservedEvent::timestamp)
                    .min(Instant::compareTo);
            if (from.isPresent() && to.isPresent()) {
                latencies.add(to.get().toEpochMilli() - from.get().toEpochMilli());
            }
        }
        return latencies;
    }

    public Map<String, List<Long>> hopLatencies() {
        Map<String, List<Long>> hops = new ConcurrentHashMap<>();
        hops.put("publish→risk", new ArrayList<>());
        hops.put("risk→routing", new ArrayList<>());
        hops.put("routing→emulator", new ArrayList<>());
        hops.put("emulator→completed", new ArrayList<>());

        for (var entry : eventsByPaymentId.entrySet()) {
            var events = entry.getValue();

            var paymentReq = topicFirst(events, "payment-requests");
            var riskAssessed = topicFirst(events, "risk-assessed");
            var routingDone = topicFirst(events, "routing-completed");
            var authDone = topicFirst(events, "authorization-completed");

            if (paymentReq.isPresent() && riskAssessed.isPresent()) {
                hops.get("publish→risk").add(riskAssessed.get().toEpochMilli() - paymentReq.get().toEpochMilli());
            }
            if (riskAssessed.isPresent() && routingDone.isPresent()) {
                hops.get("risk→routing").add(routingDone.get().toEpochMilli() - riskAssessed.get().toEpochMilli());
            }
            if (routingDone.isPresent() && authDone.isPresent()) {
                hops.get("routing→emulator").add(authDone.get().toEpochMilli() - routingDone.get().toEpochMilli());
            }
            if (paymentReq.isPresent() && authDone.isPresent()) {
                hops.get("emulator→completed")
                        .add(authDone.get().toEpochMilli() - paymentReq.get().toEpochMilli());
            }
        }
        return hops;
    }

    private Optional<Instant> topicFirst(List<ObservedEvent> events, String topic) {
        return events.stream()
                .filter(e -> topic.equals(e.topic))
                .map(ObservedEvent::timestamp)
                .min(Instant::compareTo);
    }

    public Instant lastIngestionTime() {
        return allEvents.stream()
                .filter(e -> "payment-requests".equals(e.topic))
                .map(ObservedEvent::timestamp)
                .max(Instant::compareTo)
                .orElse(observationStart);
    }

    public Instant lastCompletionTime() {
        return allEvents.stream()
                .filter(e -> "authorization-completed".equals(e.topic))
                .map(ObservedEvent::timestamp)
                .max(Instant::compareTo)
                .orElse(Instant.EPOCH);
    }

    public long drainTimeMs() {
        var lastIngest = lastIngestionTime();
        var lastComplete = lastCompletionTime();
        if (lastComplete.isAfter(Instant.EPOCH)) {
            return lastComplete.toEpochMilli() - lastIngest.toEpochMilli();
        }
        return -1;
    }

    public boolean waitForDrain(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        long ingested = ingestionCount();
        while (System.currentTimeMillis() < deadline) {
            long completed = completionCount();
            if (completed >= ingested && ingested > 0) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public void reset() {
        eventsByPaymentId.clear();
        allEvents.clear();
        observationStart = Instant.now();
        log.info("EdaObserver data reset for new scenario.");
    }

    public static long percentile(List<Long> sortedList, double p) {
        if (sortedList.isEmpty()) return -1;
        return sortedList.get((int) Math.ceil(p / 100.0 * sortedList.size()) - 1);
    }

    public long avg(List<Long> list) {
        if (list.isEmpty()) return -1;
        return list.stream().mapToLong(Long::longValue).sum() / list.size();
    }

    public record ObservedEvent(String topic, String key, GenericRecord value, Instant timestamp) {
    }
}
