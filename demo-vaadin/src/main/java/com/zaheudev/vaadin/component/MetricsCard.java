package com.zaheudev.vaadin.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.zaheudev.vaadin.model.MetricsSnapshot;
import com.zaheudev.vaadin.service.MetricsService;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MetricsCard extends VerticalLayout {

    private final MetricsService metricsService;
    private final Div content = new Div();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> refreshTask;

    public MetricsCard(MetricsService metricsService) {
        this.metricsService = metricsService;

        addClassName("card");
        setWidthFull();
        setPadding(false);
        setSpacing(false);

        add(content);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        startRefresh(attachEvent.getUI());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopRefresh();
        super.onDetach(detachEvent);
    }

    private void startRefresh(UI ui) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "metrics-card-refresh");
            t.setDaemon(true);
            return t;
        });
        refreshTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                MetricsSnapshot snapshot = metricsService.snapshot();
                ui.access(() -> render(snapshot));
            } catch (Exception e) {
                ui.access(() -> render(null));
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void stopRefresh() {
        if (refreshTask != null) refreshTask.cancel(true);
        if (scheduler != null) scheduler.shutdownNow();
        refreshTask = null;
        scheduler = null;
    }

    private void render(MetricsSnapshot s) {
        content.removeAll();
        content.setWidthFull();

        Div title = new Div();
        title.addClassName("card-title");
        title.setText("Live Metrics");
        content.add(title);

        if (s == null) {
            Paragraph err = new Paragraph("Failed to load metrics");
            err.getStyle().set("color", "var(--danger)").set("font-size", "13px");
            content.add(err);
            return;
        }

        content.add(buildTiles(s));
        content.add(buildSuccessRate(s));
        content.add(buildLatency(s));
        content.add(buildNetworkDist(s));
    }

    private Div buildTiles(MetricsSnapshot s) {
        Div row = new Div();
        row.addClassName("metric-tiles");

        row.add(buildTile("Throughput (live)", String.format("%.1f/min", s.throughputPerMinLive()), "badge-info"));
        row.add(buildTile("Throughput (cumul.)", String.format("%.1f/min", s.throughputPerMinCumulative()), "badge-info"));
        row.add(buildTile("Total payments", String.valueOf(s.totalCreated()), "badge-info"));
        row.add(buildTile("Success rate", String.format("%.1f%%", s.successRate()),
                s.successRate() >= 80 ? "badge-success" : s.successRate() >= 50 ? "badge-warning" : "badge-danger"));

        return row;
    }

    private Div buildTile(String label, String value, String badgeClass) {
        Div tile = new Div();
        tile.addClassName("metric-tile");

        Span l = new Span(label);
        l.getStyle().set("color", "var(--text-muted)").set("font-size", "12px");

        Span v = new Span(value);
        v.addClassName("badge");
        v.addClassName(badgeClass);
        v.getStyle().set("font-size", "14px").set("padding", "4px 10px");

        tile.add(l, v);
        return tile;
    }

    private Div buildSuccessRate(MetricsSnapshot s) {
        Div section = new Div();
        section.getStyle().set("margin-top", "16px");

        Div label = new Div();
        label.addClassName("card-title");
        label.setText("Success vs Rejection");
        label.getStyle().set("margin-bottom", "8px");
        section.add(label);

        long total = s.authSuccess() + s.authFailed() + s.rejected();
        if (total == 0) {
            Paragraph p = new Paragraph("No outcomes yet.");
            p.getStyle().set("color", "var(--text-muted)").set("font-size", "12px");
            section.add(p);
            return section;
        }

        double successPct = s.authSuccess() * 100.0 / total;
        double failedPct = s.authFailed() * 100.0 / total;
        double rejPct = s.rejected() * 100.0 / total;

        Div bar = new Div();
        bar.addClassName("bar");

        if (s.authSuccess() > 0) {
            Div seg = new Div();
            seg.addClassNames("bar-seg", "bar-seg-success");
            seg.getStyle().set("flex-grow", String.valueOf(s.authSuccess()));
            seg.setText(String.format("Auth OK %d (%.0f%%)", s.authSuccess(), successPct));
            bar.add(seg);
        }
        if (s.authFailed() > 0) {
            Div seg = new Div();
            seg.addClassNames("bar-seg", "bar-seg-danger");
            seg.getStyle().set("flex-grow", String.valueOf(s.authFailed()));
            seg.setText(String.format("Auth fail %d (%.0f%%)", s.authFailed(), failedPct));
            bar.add(seg);
        }
        if (s.rejected() > 0) {
            Div seg = new Div();
            seg.addClassNames("bar-seg", "bar-seg-warning");
            seg.getStyle().set("flex-grow", String.valueOf(s.rejected()));
            seg.setText(String.format("Rejected %d (%.0f%%)", s.rejected(), rejPct));
            bar.add(seg);
        }
        section.add(bar);
        return section;
    }

    private Div buildLatency(MetricsSnapshot s) {
        Div section = new Div();
        section.getStyle().set("margin-top", "16px");

        Div label = new Div();
        label.addClassName("card-title");
        label.setText("Latency per Stage");
        label.getStyle().set("margin-bottom", "8px");
        section.add(label);

        for (MetricsSnapshot.StageLatency stage : s.stages()) {
            if (stage.count() == 0) continue;
            Div row = new Div();
            row.addClassName("metric-row");

            Span name = new Span(stage.name());
            name.addClassName("metric-row-label");

            Span stats = new Span(String.format("avg %.0f ms | min %d | max %d | p95 %d | n=%d",
                    stage.avgMs(), stage.minMs(), stage.maxMs(), stage.p95Ms(), stage.count()));
            stats.addClassName("metric-row-value");
            stats.getStyle().set("font-family", "var(--mono)");

            row.add(name, stats);
            section.add(row);
        }

        if (s.stages().stream().noneMatch(st -> st.count() > 0)) {
            Paragraph p = new Paragraph("No latency data yet.");
            p.getStyle().set("color", "var(--text-muted)").set("font-size", "12px");
            section.add(p);
        }

        return section;
    }

    private Div buildNetworkDist(MetricsSnapshot s) {
        Div section = new Div();
        section.getStyle().set("margin-top", "16px");

        Div label = new Div();
        label.addClassName("card-title");
        label.setText("Network Distribution");
        label.getStyle().set("margin-bottom", "8px");
        section.add(label);

        if (s.networkCounts().isEmpty()) {
            Paragraph p = new Paragraph("No routing events yet.");
            p.getStyle().set("color", "var(--text-muted)").set("font-size", "12px");
            section.add(p);
            return section;
        }

        long total = s.networkCounts().values().stream().mapToLong(l -> l).sum();

        for (Map.Entry<String, Long> entry : s.networkCounts().entrySet()) {
            String network = entry.getKey();
            long count = entry.getValue();
            double pct = count * 100.0 / total;

            Div row = new Div();
            row.addClassName("net-row");

            Span name = new Span(network);
            name.addClassName("net-label");

            Div barTrack = new Div();
            barTrack.addClassName("net-bar-track");
            Div barFill = new Div();
            barFill.addClassName("net-bar");
            barFill.getStyle().set("width", String.format("%.0f%%", pct));
            barTrack.add(barFill);

            Span val = new Span(String.format("%d (%.1f%%)", count, pct));
            val.addClassName("net-value");

            row.add(name, barTrack, val);
            section.add(row);
        }

        return section;
    }
}
