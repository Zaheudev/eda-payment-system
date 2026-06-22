package com.zaheudev.vaadin.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.zaheudev.vaadin.client.PaymentClient;

import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TotalFees extends VerticalLayout {

    private final PaymentClient paymentClient;
    private BigDecimal optimalTotal = BigDecimal.ZERO;
    private BigDecimal suboptimalTotal = BigDecimal.ZERO;
    private String errorMessage = null;

    private final Div content = new Div();
    
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> refreshTask;

    public TotalFees(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;

        addClassName("card");
        setWidthFull();
        setPadding(false);
        setSpacing(false);

        buildDetails();
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
            Thread t = new Thread(r, "total-fees-refresh");
            t.setDaemon(true);
            return t;
        });
        refreshTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                BigDecimal[] costs = paymentClient.fetchFeeCosts();
                BigDecimal optimal = costs != null && costs.length > 0 ? costs[0] : BigDecimal.ZERO;
                BigDecimal suboptimal = costs != null && costs.length > 1 ? costs[1] : BigDecimal.ZERO;
                ui.access(() -> {
                    optimalTotal = optimal;
                    suboptimalTotal = suboptimal;
                    errorMessage = null;
                    buildDetails();
                });
            } catch (Exception e) {
                ui.access(() -> {
                    errorMessage = "Failed to load total fees";
                    buildDetails();
                });
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private void stopRefresh() {
        if (refreshTask != null) refreshTask.cancel(true);
        if (scheduler != null) scheduler.shutdownNow();
        refreshTask = null;
        scheduler = null;
    }

    private void buildDetails() {
        content.removeAll();
        content.setWidthFull();

        Div title = new Div();
        title.addClassName("card-title");
        title.setText("Total Fees");
        content.add(title);

        Paragraph subtitle = new Paragraph("Cumulative routing fees: optimal vs suboptimal network");
        subtitle.getStyle().set("color", "var(--text-muted)").set("font-size", "12px").set("margin-top", "-4px").set("margin-bottom", "12px");
        content.add(subtitle);

        if (errorMessage != null) {
            Paragraph err = new Paragraph(errorMessage);
            err.getStyle().set("color", "var(--danger)").set("font-size", "13px");
            content.add(err);
            return;
        }

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.setAlignItems(Alignment.CENTER);

        row.add(buildMetric("Optimal routing", fmt(optimalTotal), "badge-success"));
        row.add(buildMetric("Suboptimal routing", fmt(suboptimalTotal), "badge-danger"));

        BigDecimal savings = suboptimalTotal.subtract(optimalTotal);
        if (savings.compareTo(BigDecimal.ZERO) > 0) {
            Div save = new Div();
            save.getStyle().set("margin-left", "auto");
            Span saveLabel = new Span("You save");
            saveLabel.getStyle().set("color", "var(--text-muted)").set("font-size", "12px").set("margin-right", "6px");
            Span saveVal = new Span(fmt(savings));
            saveVal.addClassName("badge");
            saveVal.addClassName("badge-success");
            save.add(saveLabel, saveVal);
            row.add(save);
        }

        content.add(row);
    }

    private Div buildMetric(String label, String value, String badgeClass) {
        Div metric = new Div();
        metric.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "4px");

        Span l = new Span(label);
        l.getStyle().set("color", "var(--text-muted)").set("font-size", "12px");

        Span v = new Span(value);
        v.addClassName("badge");
        v.addClassName(badgeClass);
        v.getStyle().set("font-size", "14px").set("padding", "4px 10px");

        metric.add(l, v);
        return metric;
    }

    private static String fmt(BigDecimal v) {
        return v == null ? "0.00" : String.format("%.2f", v);
    }
}
