package com.zaheudev.vaadin.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.grid.Grid;
import com.zaheudev.vaadin.client.PaymentClient;
import com.zaheudev.vaadin.model.PaymentResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PaymentsGrid extends VerticalLayout {

    private final PaymentClient paymentClient;
    private final Grid<PaymentResponse> grid = new Grid<>(PaymentResponse.class, false);
    private final Span titleSpan = new Span("Payments (latest 20)");

    private java.util.function.Consumer<String> onSelectPayment;
    private ScheduledFuture<?> pollFuture;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "payments-poller");
        t.setDaemon(true);
        return t;
    });

    public PaymentsGrid(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
        addClassName("card");
        configureGrid();
        layoutHeader();
        add(titleRow(), grid);
        setPadding(false);
        setSpacing(false);
        refresh();
    }

    private void configureGrid() {
        grid.addColumn(r -> {
            String pid = r.paymentId();
            return pid != null && pid.length() > 16 ? pid.substring(0, 16) + "..." : pid;
        }).setHeader("Payment ID").setAutoWidth(true);

        grid.addComponentColumn(r -> {
            Span badge = new Span(r.paymentStatus());
            badge.addClassName("badge");
            badge.addClassName(statusClass(r.paymentStatus()));
            return badge;
        }).setHeader("Status").setAutoWidth(true);

        grid.addColumn(r -> {
            PaymentResponse.Amount amt = r.amount();
            if (amt == null) return "-";
            return String.format("%.2f %s", amt.amount() / 100.0, amt.currency());
        }).setHeader("Amount").setAutoWidth(true);

        grid.addColumn(r -> {
            if (r.createdAt() == null) return "-";
            return formatCreatedAt(r.createdAt());
        }).setHeader("Created").setAutoWidth(true);

        grid.addClassName("payments-scroll");
        grid.addItemClickListener(e -> {
            if (onSelectPayment != null && e.getItem() != null) {
                onSelectPayment.accept(e.getItem().paymentId());
            }
        });
    }

    private HorizontalLayout titleRow() {
        Button refreshBtn = new Button("Refresh");
        refreshBtn.addClassName("btn-secondary");
        refreshBtn.getStyle().set("font-size", "12px");
        refreshBtn.addClickListener(e -> refresh());

        titleSpan.addClassName("card-title");
        HorizontalLayout row = new HorizontalLayout(titleSpan, refreshBtn);
        row.setWidthFull();
        row.setJustifyContentMode(JustifyContentMode.BETWEEN);
        row.setAlignItems(Alignment.CENTER);
        return row;
    }

    private void layoutHeader() {
        // titleRow already added in constructor
    }

    private String statusClass(String status) {
        return switch (status) {
            case "AUTHORIZED", "CAPTURED", "REFUNDED" -> "badge-success";
            case "REJECTED", "FAILED" -> "badge-danger";
            case "VOID" -> "badge-warning";
            default -> "badge-info";
        };
    }

    public void refresh() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                List<PaymentResponse> payments = paymentClient.fetchAllPayments(20);
                payments.sort(Comparator.comparing(
                        r -> r.createdAt() != null ? r.createdAt() : "",
                        Comparator.reverseOrder()));
                grid.setItems(payments);
            } catch (Exception ignored) {
            }
        }));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        pollFuture = scheduler.scheduleAtFixedRate(
                () -> getUI().ifPresent(ui -> ui.access(this::refresh)),
                5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pollFuture != null) {
            pollFuture.cancel(false);
        }
        super.onDetach(detachEvent);
    }

    public void setOnSelectPayment(java.util.function.Consumer<String> listener) {
        this.onSelectPayment = listener;
    }

    private static String formatCreatedAt(String s) {
        try {
            Instant instant = Instant.parse(s);
            return instant.toString().replace("T", " ").substring(0, 19);
        } catch (DateTimeParseException e) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(s);
                return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException ex) {
                return s;
            }
        }
    }
}
