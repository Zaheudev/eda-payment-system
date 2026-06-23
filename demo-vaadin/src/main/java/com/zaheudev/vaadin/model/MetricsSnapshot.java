package com.zaheudev.vaadin.model;

import java.util.List;
import java.util.Map;

public record MetricsSnapshot(
        long totalCreated,
        double throughputPerMinLive,
        double throughputPerMinCumulative,
        long authSuccess,
        long authFailed,
        long rejected,
        double successRate,
        List<StageLatency> stages,
        Map<String, Long> networkCounts
) {
    public record StageLatency(
            String name,
            double avgMs,
            long minMs,
            long maxMs,
            long p95Ms,
            long count
    ) {}
}
