package com.zaheudev.vaadin.service;

import com.zaheudev.vaadin.model.EventEnvelope;
import com.zaheudev.vaadin.model.MetricsSnapshot;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {

    private static final int MAX_SAMPLES = 2000;
    private static final long WINDOW_MS = 60_000L;

    private static final String STAGE_RISK = "Created \u2192 Risk";
    private static final String STAGE_ROUTE = "Risk \u2192 Routing";
    private static final String STAGE_AUTH = "Routing \u2192 Auth";
    private static final String STAGE_TOTAL = "Created \u2192 Auth (total)";

    private final Instant startTime = Instant.now();

    private final AtomicLong totalCreated = new AtomicLong();
    private final ConcurrentLinkedDeque<Long> createdTs = new ConcurrentLinkedDeque<>();

    private final AtomicLong authSuccess = new AtomicLong();
    private final AtomicLong authFailed = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();

    private final ConcurrentHashMap<String, AtomicLong> networkCounts = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, long[]> stageTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> stageDeltas = new ConcurrentHashMap<>();

    public void onEvent(EventEnvelope e) {
        if (e == null || e.getTopic() == null) return;
        String pid = e.getPaymentId();
        Map<String, Object> p = asMap(e.getPayload());

        switch (e.getTopic()) {
            case "payment-requests" -> {
                totalCreated.incrementAndGet();
                createdTs.addLast(e.getTimestamp());
                if (pid != null) {
                    long[] ts = new long[4];
                    ts[0] = e.getTimestamp();
                    stageTimestamps.put(pid, ts);
                }
            }
            case "risk-assessed" -> {
                if (pid != null) recordDelta(pid, 1, e.getTimestamp(), STAGE_RISK);
            }
            case "routing-completed" -> {
                if (pid != null) recordDelta(pid, 2, e.getTimestamp(), STAGE_ROUTE);
                String network = str(p, "selectedPaymentMethod");
                if (network != null && !network.isBlank()) {
                    networkCounts.computeIfAbsent(network, k -> new AtomicLong()).incrementAndGet();
                }
            }
            case "authorization-completed" -> {
                if (pid != null) {
                    long[] ts = stageTimestamps.get(pid);
                    if (ts != null && ts[0] > 0) {
                        addDelta(STAGE_TOTAL, e.getTimestamp() - ts[0]);
                    }
                    recordDelta(pid, 3, e.getTimestamp(), STAGE_AUTH);
                    stageTimestamps.remove(pid);
                }
                if (bool(p, "success")) authSuccess.incrementAndGet();
                else authFailed.incrementAndGet();
            }
            case "payment-rejected" -> {
                if (pid != null) stageTimestamps.remove(pid);
                rejected.incrementAndGet();
            }
            default -> {}
        }
    }

    private void recordDelta(String pid, int stageIndex, long timestamp, String stageName) {
        long[] ts = stageTimestamps.get(pid);
        if (ts == null || ts[stageIndex - 1] == 0) return;
        long delta = timestamp - ts[stageIndex - 1];
        if (delta >= 0) {
            ts[stageIndex] = timestamp;
            addDelta(stageName, delta);
        }
    }

    private void addDelta(String stageName, long deltaMs) {
        ConcurrentLinkedDeque<Long> deque = stageDeltas.computeIfAbsent(stageName, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(deltaMs);
        while (deque.size() > MAX_SAMPLES) deque.pollFirst();
    }

    public MetricsSnapshot snapshot() {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS;
        long liveCount = 0;
        for (Iterator<Long> it = createdTs.iterator(); it.hasNext(); ) {
            long ts = it.next();
            if (ts < cutoff) {
                it.remove();
            } else {
                liveCount++;
            }
        }
        double throughputLive = (liveCount / (double) WINDOW_MS) * 60_000.0;

        long total = totalCreated.get();
        long elapsedMs = Duration.between(startTime, Instant.now()).toMillis();
        double throughputCumulative = elapsedMs > 0
                ? (total / (double) elapsedMs) * 60_000.0 : 0.0;

        long success = authSuccess.get();
        long failed = authFailed.get();
        long rej = rejected.get();
        long outcomes = success + failed + rej;
        double successRate = outcomes > 0 ? (success * 100.0 / outcomes) : 0.0;

        List<MetricsSnapshot.StageLatency> stages = List.of(
                buildStage(STAGE_RISK),
                buildStage(STAGE_ROUTE),
                buildStage(STAGE_AUTH),
                buildStage(STAGE_TOTAL)
        );

        Map<String, Long> networks = new LinkedHashMap<>();
        networkCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                .forEach(e -> networks.put(e.getKey(), e.getValue().get()));

        return new MetricsSnapshot(
                total,
                throughputLive,
                throughputCumulative,
                success,
                failed,
                rej,
                successRate,
                stages,
                networks
        );
    }

    private MetricsSnapshot.StageLatency buildStage(String name) {
        ConcurrentLinkedDeque<Long> deque = stageDeltas.get(name);
        if (deque == null || deque.isEmpty()) {
            return new MetricsSnapshot.StageLatency(name, 0, 0, 0, 0, 0);
        }
        List<Long> sorted = new ArrayList<>(deque);
        Collections.sort(sorted);
        int n = sorted.size();
        double avg = sorted.stream().mapToLong(l -> l).average().orElse(0);
        long min = sorted.get(0);
        long max = sorted.get(n - 1);
        int p95Idx = (int) Math.min(n - 1, Math.ceil(n * 0.95) - 1);
        long p95 = sorted.get(p95Idx);
        return new MetricsSnapshot.StageLatency(name, avg, min, max, p95, n);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object payload) {
        if (payload instanceof Map) return (Map<String, Object>) payload;
        return Map.of();
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    private boolean bool(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Boolean b ? b : false;
    }
}
