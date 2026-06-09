package com.zaheudev.integration.metrics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsCollector {

    private final ConcurrentHashMap<String, ScenarioMetrics> scenarios = new ConcurrentHashMap<>();

    public ScenarioMetrics getOrCreate(String scenarioName) {
        return scenarios.computeIfAbsent(scenarioName, ScenarioMetrics::new);
    }

    public Collection<ScenarioMetrics> getAll() {
        return scenarios.values();
    }

    public static class ScenarioMetrics {
        private final String name;
        private final List<Long> endToEndLatencies = Collections.synchronizedList(new ArrayList<>());
        private final Map<String, List<Long>> hopLatencies = new ConcurrentHashMap<>();
        private long recoveryTimeMs = -1;
        private long throughputPerSec = -1;
        private long processedCount = 0;
        private long errorCount = 0;
        private long totalDurationMs = -1;
        private String status = "RUNNING";
        private final List<String> notes = Collections.synchronizedList(new ArrayList<>());

        public ScenarioMetrics(String name) { this.name = name; }

        public void addEndToEndLatency(long ms) { endToEndLatencies.add(ms); }
        public void addHopLatency(String hop, long ms) {
            hopLatencies.computeIfAbsent(hop, k -> Collections.synchronizedList(new ArrayList<>())).add(ms);
        }
        public void setRecoveryTimeMs(long ms) { this.recoveryTimeMs = ms; }
        public void setThroughputPerSec(long tps) { this.throughputPerSec = tps; }
        public void incrementProcessed() { processedCount++; }
        public void incrementError() { errorCount++; }
        public void setTotalDurationMs(long ms) { this.totalDurationMs = ms; }
        public void setStatus(String s) { this.status = s; }
        public void addNote(String note) { notes.add(note); }

        public String getName() { return name; }
        public long percentile(List<Long> list, double p) {
            if (list.isEmpty()) return -1;
            var sorted = new ArrayList<>(list);
            Collections.sort(sorted);
            return sorted.get((int) Math.ceil(p / 100.0 * sorted.size()) - 1);
        }

        public Map<String, Long> p50HopLatencies() {
            Map<String, Long> m = new LinkedHashMap<>();
            hopLatencies.forEach((k, v) -> m.put(k, percentile(v, 50)));
            return m;
        }

        public long p50() { return percentile(endToEndLatencies, 50); }
        public long p95() { return percentile(endToEndLatencies, 95); }
        public long p99() { return percentile(endToEndLatencies, 99); }
        public long avgLatency() {
            return endToEndLatencies.isEmpty() ? -1
                    : endToEndLatencies.stream().mapToLong(Long::longValue).sum() / endToEndLatencies.size();
        }
        public long getRecoveryTimeMs() { return recoveryTimeMs; }
        public long getThroughputPerSec() { return throughputPerSec; }
        public long getProcessedCount() { return processedCount; }
        public long getErrorCount() { return errorCount; }
        public long getTotalDurationMs() { return totalDurationMs; }
        public String getStatus() { return status; }
        public List<String> getNotes() { return new ArrayList<>(notes); }
        public int getRecordedEventCount() { return endToEndLatencies.size(); }
    }
}
