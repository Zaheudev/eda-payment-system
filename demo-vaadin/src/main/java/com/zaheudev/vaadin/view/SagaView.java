package com.zaheudev.vaadin.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.zaheudev.vaadin.model.SagaState;
import com.zaheudev.vaadin.service.SagaProjectionService;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SagaView extends VerticalLayout {

    private static final List<String> ALL_STATES = List.of(
            "CREATED", "RISK_ASSESSED", "ROUTED", "AUTHORIZED",
            "CAPTURED", "VOIDED", "REFUNDED", "REJECTED"
    );

    private final SagaProjectionService sagaService;
    private final Grid<SagaState> grid = new Grid<>(SagaState.class, false);
    private final Div detailPanel = new Div();

    private ScheduledFuture<?> pollFuture;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "saga-poller");
        t.setDaemon(true);
        return t;
    });

    public SagaView(SagaProjectionService sagaService) {
        this.sagaService = sagaService;
        setWidthFull();
        setSpacing(false);
        setPadding(false);

        configureGrid();

        HorizontalLayout cols = new HorizontalLayout(buildGridCard(), buildDetailCard());
        cols.setWidthFull();
        cols.setSpacing(true);
        add(cols);
    }

    private Div buildGridCard() {
        Div card = new Div();
        card.addClassName("card");
        card.setWidth("50%");

        Div title = new Div();
        title.addClassName("card-title");
        title.setText("All Sagas");

        card.add(title, grid);
        return card;
    }

    private Div buildDetailCard() {
        detailPanel.addClassName("card");
        detailPanel.setWidth("50%");

        Div title = new Div();
        title.addClassName("card-title");
        title.setText("Saga Detail");

        Paragraph placeholder = new Paragraph("Select a saga from the list.");
        placeholder.getStyle().set("color", "var(--text-muted)").set("font-size", "13px");
        detailPanel.add(title, placeholder);

        return detailPanel;
    }

    private void configureGrid() {
        grid.addColumn(s -> {
            String pid = s.getPaymentId();
            return pid != null && pid.length() > 16 ? pid.substring(0, 16) + "..." : pid;
        }).setHeader("Payment ID").setAutoWidth(true);

        grid.addComponentColumn(s -> {
            Span badge = new Span(s.getCurrentState());
            badge.addClassName("badge");
            badge.addClassName(statusClass(s.getCurrentState()));
            return badge;
        }).setHeader("Current State").setAutoWidth(true);

        grid.addComponentColumn(s -> {
            Button btn = new Button("View");
            btn.addClassName("btn-secondary");
            btn.getStyle().set("font-size", "12px");
            btn.addClickListener(e -> showDetail(s.getPaymentId()));
            return btn;
        }).setHeader("Actions").setAutoWidth(true);

        grid.addClassName("payments-scroll");

        refresh();
    }

    private void refresh() {
        getUI().ifPresent(ui -> ui.access(() -> {
            List<SagaState> sagaList = new ArrayList<>(sagaService.getAllSagas());
            sagaList.sort(Comparator.comparing(
                    s -> s.getLastUpdatedAt() != null ? s.getLastUpdatedAt() : java.time.Instant.EPOCH,
                    Comparator.reverseOrder()));
            grid.setItems(sagaList.stream().limit(20).toList());
        }));
    }

    private void showDetail(String paymentId) {
        SagaState state = sagaService.getSaga(paymentId);
        detailPanel.removeAll();

        Div title = new Div();
        title.addClassName("card-title");
        title.setText("Saga Detail");
        detailPanel.add(title);

        if (state == null) {
            Paragraph p = new Paragraph("Saga not found.");
            p.getStyle().set("color", "var(--text-muted)").set("font-size", "13px");
            detailPanel.add(p);
            return;
        }

        Paragraph pidP = new Paragraph();
        pidP.getElement().setProperty("innerHTML", "<strong>" + state.getPaymentId() + "</strong>");
        pidP.getStyle().set("font-size", "13px").set("margin-bottom", "16px");
        detailPanel.add(pidP);

        Div flow = new Div();
        flow.addClassName("saga-flow");

        for (int i = 0; i < ALL_STATES.size(); i++) {
            String st = ALL_STATES.get(i);
            int idx = state.getHistory().indexOf(st);
            boolean isCurrent = state.getCurrentState().equals(st);
            boolean isDone = idx >= 0 || (state.getHistory().size() > 0
                    && ALL_STATES.indexOf(state.getCurrentState()) > ALL_STATES.indexOf(st));
            boolean isRejected = state.getCurrentState().equals("REJECTED") && st.equals("REJECTED");

            if (i > 0) {
                Span arrow = new Span("\u2192");
                arrow.addClassName("saga-arrow");
                flow.add(arrow);
            }

            Span node = new Span(st);
            node.addClassName("saga-node");
            if (isRejected) node.addClassName("rejected");
            else if (isCurrent) node.addClassName("active");
            else if (isDone) node.addClassName(st.equals("REJECTED") ? "rejected" : "done");

            flow.add(node);
        }
        detailPanel.add(flow);

        String historyStr = String.join(" > ", state.getHistory()) + " \u2192 " + state.getCurrentState();
        Paragraph histP = new Paragraph("History: " + historyStr);
        histP.getStyle().set("margin-top", "12px").set("font-size", "12px").set("color", "var(--text-muted)");
        detailPanel.add(histP);
    }

    private String statusClass(String state) {
        return switch (state) {
            case "CAPTURED" -> "badge-success";
            case "REJECTED" -> "badge-danger";
            default -> "badge-info";
        };
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        pollFuture = scheduler.scheduleAtFixedRate(
                () -> getUI().ifPresent(ui -> ui.access(this::refresh)),
                3, 3, TimeUnit.SECONDS);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pollFuture != null) pollFuture.cancel(false);
        super.onDetach(detachEvent);
    }
}
