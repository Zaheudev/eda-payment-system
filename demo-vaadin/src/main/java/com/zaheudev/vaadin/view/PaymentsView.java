package com.zaheudev.vaadin.view;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.zaheudev.vaadin.client.PaymentClient;
import com.zaheudev.vaadin.component.LiveTracker;
import com.zaheudev.vaadin.component.MetricsCard;
import com.zaheudev.vaadin.component.PaymentFormComponent;
import com.zaheudev.vaadin.component.PaymentsGrid;
import com.zaheudev.vaadin.component.TotalFees;
import com.zaheudev.vaadin.service.EventBroadcaster;
import com.zaheudev.vaadin.service.MetricsService;
import com.zaheudev.vaadin.service.PaymentEventReplayService;

public class PaymentsView extends VerticalLayout {

    private final LiveTracker tracker;
    private final TotalFees totalFees;
    private final MetricsCard metricsCard;

    public PaymentsView(PaymentClient paymentClient, EventBroadcaster broadcaster,
                        PaymentEventReplayService replayService, MetricsService metricsService) {
        setWidthFull();
        setSpacing(true);
        setPadding(false);

        PaymentFormComponent form = new PaymentFormComponent(paymentClient);
        PaymentsGrid grid = new PaymentsGrid(paymentClient);
        tracker = new LiveTracker(paymentClient, broadcaster, replayService);
        totalFees = new TotalFees(paymentClient);
        metricsCard = new MetricsCard(metricsService);

        form.setOnSuccess(grid::refresh);
        form.setOnTrack(tracker::startTracking);
        grid.setOnSelectPayment(tracker::startTracking);

        tracker.setOnStop(() -> {});

        VerticalLayout topRow = new VerticalLayout();
        topRow.setWidthFull();
        topRow.setPadding(false);
        topRow.setSpacing(true);

        topRow.getStyle().set("display", "grid");
        topRow.getStyle().set("grid-template-columns", "1fr 1fr");
        topRow.getStyle().set("gap", "16px");

        topRow.add(form, grid);
        add(topRow, totalFees, metricsCard, tracker);
    }

    public void startTracking(String paymentId) {
        tracker.startTracking(paymentId);
    }
}
