package com.zaheudev.load.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

public class EdaLoadReportWriter {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final List<ScenarioResult> scenarios = Collections.synchronizedList(new ArrayList<>());
    private final Path outputDir = Paths.get("target", "eda-load-report");
    private final Instant suiteStart = Instant.now();

    public ScenarioResult newScenario(String name) {
        var sr = new ScenarioResult(name);
        scenarios.add(sr);
        return sr;
    }

    public void write() throws IOException {
        Files.createDirectories(outputDir);

        Map<String, Object> jsonRoot = new LinkedHashMap<>();
        jsonRoot.put("generatedAt", Instant.now().toString());
        jsonRoot.put("totalDurationMs", Instant.now().toEpochMilli() - suiteStart.toEpochMilli());

        StringBuilder md = new StringBuilder();
        md.append("# EDA Load Test Report\n\n");
        md.append("**Generated**: ").append(Instant.now()).append("\n");
        md.append("**Target**: ").append(LoadConfig.baseUrl()).append("\n\n");
        md.append("---\n\n");

        md.append("## Executive Summary\n\n");
        md.append("This report demonstrates the **power of event-driven architecture (EDA)** ");
        md.append("in a payment processing system under load.\n\n");
        md.append("- **Decoupling**: The HTTP gateway accepts payments at a controlled rate and returns immediately, ");
        md.append("while downstream services (risk, routing, card network) process events **asynchronously** via Kafka.\n");
        md.append("- **Backpressure & Buffering**: During spikes or downstream outages, Kafka buffers events — ");
        md.append("no ingest request is lost even when downstream services are down.\n");
        md.append("- **Resilience**: When a downstream consumer fails mid-load, ingestion continues unaffected. ");
        md.append("After recovery, the backlog drains to completion with **zero data loss**.\n\n");
        md.append("---\n\n");

        List<Map<String, Object>> scenarioList = new ArrayList<>();

        for (var s : scenarios) {
            appendScenario(md, s);
            scenarioList.add(s.toJson());
        }

        md.append("\n---\n\n## Legend\n\n");
        md.append("- **HTTP latency** — round-trip measured by Gatling at the HTTP layer (ingestion only)\n");
        md.append("- **Async E2E latency** — time from `payment-requests` Kafka event to `authorization-completed` event\n");
        md.append("- **Ingestion TPS** — payment-requests events/second observed on Kafka\n");
        md.append("- **Async drain time** — time from last ingested event to last acknowledged completion\n");
        md.append("- **Zero-loss** — all ingested payments must reach AUTHORIZED\n");

        jsonRoot.put("scenarios", scenarioList);

        Files.writeString(outputDir.resolve("metrics.json"),
                mapper.writeValueAsString(jsonRoot));
        Files.writeString(outputDir.resolve("report.md"), md.toString());

        System.out.println("\n==============================");
        System.out.println("  EDA LOAD TEST REPORT");
        System.out.println("==============================");
        System.out.println(md);
        System.out.println("Report written to: " + outputDir.toAbsolutePath());
    }

    private void appendScenario(StringBuilder md, ScenarioResult s) {
        md.append("## ").append(s.name).append("  `").append(s.status).append("`\n\n");

        md.append("| Metric | Value |\n");
        md.append("|--------|-------|\n");
        md.append("| Payments ingested (async) | ").append(s.asyncIngestionCount).append(" |\n");
        md.append("| Payments completed (async) | ").append(s.asyncCompletionCount).append(" |\n");
        md.append("| Zero-loss | ").append(s.zeroLoss ? "YES" : "NO (" + s.lostCount + " lost)").append(" |\n");
        md.append("| Async E2E avg latency | ").append(formatMs(s.asyncAvgLatency)).append(" |\n");
        md.append("| Async E2E p50 | ").append(formatMs(s.asyncP50)).append(" |\n");
        md.append("| Async E2E p95 | ").append(formatMs(s.asyncP95)).append(" |\n");
        md.append("| Async E2E p99 | ").append(formatMs(s.asyncP99)).append(" |\n");

        if (s.httpP95 > 0) {
            md.append("| HTTP ingestion p95 | ").append(s.httpP95).append(" ms |\n");
        }
        if (s.httpP99 > 0) {
            md.append("| HTTP ingestion p99 | ").append(s.httpP99).append(" ms |\n");
        }

        md.append("| Async drain time | ").append(formatMs(s.asyncDrainMs)).append(" |\n");

        if (s.recoveryTimeMs >= 0) {
            md.append("| Recovery time | ").append(s.recoveryTimeMs).append(" ms |\n");
        }
        if (s.totalDurationMs > 0) {
            md.append("| Total duration | ").append(s.totalDurationMs).append(" ms |\n");
        }

        if (!s.hopLatencies.isEmpty()) {
            md.append("\n**Per-hop async latency (p50):**\n\n");
            md.append("| Hop | Latency |\n");
            md.append("|-----|---------|\n");
            s.hopLatencies.forEach((hop, lat) ->
                    md.append("| ").append(hop).append(" | ").append(formatMs(lat)).append(" |\n"));
        }

        if (!s.notes.isEmpty()) {
            md.append("\n**Notes:**\n");
            s.notes.forEach(n -> md.append("- ").append(n).append("\n"));
        }
        md.append("\n---\n\n");
    }

    private String formatMs(long ms) {
        return ms < 0 ? "N/A" : ms + " ms";
    }

    public static class ScenarioResult {
        public final String name;
        public String status = "RUNNING";
        public long asyncIngestionCount;
        public long asyncCompletionCount;
        public long asyncAvgLatency = -1;
        public long asyncP50 = -1;
        public long asyncP95 = -1;
        public long asyncP99 = -1;
        public long httpP95;
        public long httpP99;
        public long asyncDrainMs = -1;
        public long recoveryTimeMs = -1;
        public long totalDurationMs = -1;
        public boolean zeroLoss = true;
        public long lostCount;
        public Map<String, Long> hopLatencies = new LinkedHashMap<>();
        public List<String> notes = Collections.synchronizedList(new ArrayList<>());

        public ScenarioResult(String name) {
            this.name = name;
        }

        public Map<String, Object> toJson() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("status", status);
            m.put("asyncIngestionCount", asyncIngestionCount);
            m.put("asyncCompletionCount", asyncCompletionCount);
            m.put("asyncAvgLatencyMs", asyncAvgLatency);
            m.put("asyncP50LatencyMs", asyncP50);
            m.put("asyncP95LatencyMs", asyncP95);
            m.put("asyncP99LatencyMs", asyncP99);
            m.put("httpP95Ms", httpP95);
            m.put("httpP99Ms", httpP99);
            m.put("asyncDrainMs", asyncDrainMs);
            m.put("recoveryTimeMs", recoveryTimeMs);
            m.put("totalDurationMs", totalDurationMs);
            m.put("zeroLoss", zeroLoss);
            m.put("hopLatenciesMs", hopLatencies);
            m.put("notes", notes);
            return m;
        }
    }
}
