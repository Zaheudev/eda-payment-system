package com.zaheudev.integration.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

public class ReportWriter {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final MetricsCollector collector;
    private final Path outputDir;
    private final Instant suiteStart;

    public ReportWriter(MetricsCollector collector) {
        this.collector = collector;
        this.outputDir = Paths.get("target", "eda-report");
        this.suiteStart = Instant.now();
    }

    public void write() throws IOException {
        Files.createDirectories(outputDir);

        Map<String, Object> jsonRoot = new LinkedHashMap<>();
        jsonRoot.put("generatedAt", Instant.now().toString());
        jsonRoot.put("totalDurationMs", Instant.now().toEpochMilli() - suiteStart.toEpochMilli());

        StringBuilder md = new StringBuilder();
        md.append("# EDA Integration Test Report\n\n");
        md.append("**Generated**: ").append(Instant.now()).append("\n\n");
        md.append("---\n\n");

        List<Map<String, Object>> scenarioList = new ArrayList<>();

        for (var m : collector.getAll()) {
            appendScenario(md, m);

            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("name", m.getName());
            sm.put("status", m.getStatus());
            sm.put("processedCount", m.getProcessedCount());
            sm.put("errorCount", m.getErrorCount());
            sm.put("avgLatencyMs", m.avgLatency());
            sm.put("p50LatencyMs", m.p50());
            sm.put("p95LatencyMs", m.p95());
            sm.put("p99LatencyMs", m.p99());
            sm.put("throughputPerSec", m.getThroughputPerSec());
            sm.put("recoveryTimeMs", m.getRecoveryTimeMs());
            sm.put("totalDurationMs", m.getTotalDurationMs());
            sm.put("hopLatenciesMs", m.p50HopLatencies());
            sm.put("notes", m.getNotes());
            scenarioList.add(sm);
        }

        md.append("\n---\n\n## Legend\n\n");
        md.append("- **E2E latency** = time from `payment-requests` publish to `authorization-completed` receipt\n");
        md.append("- **Throughput** = payments/second during sustained load\n");
        md.append("- **Recovery time** = time from context restart to last backlog event consumed\n");
        md.append("- **p50/p95/p99** = latency percentiles across all sampled events\n");

        jsonRoot.put("scenarios", scenarioList);

        Files.writeString(outputDir.resolve("metrics.json"),
                mapper.writeValueAsString(jsonRoot));
        Files.writeString(outputDir.resolve("report.md"), md.toString());

        System.out.println("\n==============================");
        System.out.println("  EDA INTEGRATION TEST REPORT");
        System.out.println("==============================");
        System.out.println(md);
        System.out.println("Report written to: " + outputDir.toAbsolutePath());
    }

    private void appendScenario(StringBuilder md, MetricsCollector.ScenarioMetrics m) {
        md.append("## ").append(m.getName()).append("  ");
        md.append("`").append(m.getStatus()).append("`\n\n");

        md.append("| Metric | Value |\n");
        md.append("|--------|-------|\n");
        md.append("| Payments processed | ").append(m.getProcessedCount()).append(" |\n");
        md.append("| Errors | ").append(m.getErrorCount()).append(" |\n");
        md.append("| Avg E2E latency | ").append(formatMs(m.avgLatency())).append(" |\n");
        md.append("| p50 latency | ").append(formatMs(m.p50())).append(" |\n");
        md.append("| p95 latency | ").append(formatMs(m.p95())).append(" |\n");
        md.append("| p99 latency | ").append(formatMs(m.p99())).append(" |\n");

        if (m.getThroughputPerSec() > 0) {
            md.append("| Throughput | ").append(m.getThroughputPerSec()).append(" tx/s |\n");
        }
        if (m.getRecoveryTimeMs() >= 0) {
            md.append("| Recovery time | ").append(m.getRecoveryTimeMs()).append(" ms |\n");
        }
        if (m.getTotalDurationMs() > 0) {
            md.append("| Total duration | ").append(m.getTotalDurationMs()).append(" ms |\n");
        }

        var hops = m.p50HopLatencies();
        if (!hops.isEmpty()) {
            md.append("\n**Per-hop latency (p50):**\n\n");
            md.append("| Hop | Latency |\n");
            md.append("|-----|---------|\n");
            hops.forEach((hop, lat) -> md.append("| ").append(hop).append(" | ").append(formatMs(lat)).append(" |\n"));
        }

        if (!m.getNotes().isEmpty()) {
            md.append("\n**Notes:**\n");
            m.getNotes().forEach(n -> md.append("- ").append(n).append("\n"));
        }
        md.append("\n---\n\n");
    }

    private String formatMs(long ms) {
        return ms < 0 ? "N/A" : ms + " ms";
    }
}
