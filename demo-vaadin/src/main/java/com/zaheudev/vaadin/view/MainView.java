package com.zaheudev.vaadin.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import com.zaheudev.vaadin.client.PaymentClient;
import com.zaheudev.vaadin.service.EventBroadcaster;
import com.zaheudev.vaadin.service.MetricsService;
import com.zaheudev.vaadin.service.PaymentEventReplayService;
import com.zaheudev.vaadin.service.SagaProjectionService;

@Route("")
public class MainView extends VerticalLayout {

    private final PaymentClient paymentClient;
    private final EventBroadcaster broadcaster;
    private final SagaProjectionService sagaService;
    private final PaymentEventReplayService replayService;
    private final MetricsService metricsService;

    private final Div contentArea = new Div();

    public MainView(PaymentClient paymentClient, EventBroadcaster broadcaster,
                    SagaProjectionService sagaService, PaymentEventReplayService replayService,
                    MetricsService metricsService) {
        this.paymentClient = paymentClient;
        this.broadcaster = broadcaster;
        this.sagaService = sagaService;
        this.replayService = replayService;
        this.metricsService = metricsService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("app-layout");

        Div header = buildHeader();
        add(header);

        contentArea.setWidthFull();
        contentArea.getStyle().set("padding", "16px 24px");
        contentArea.getStyle().set("flex", "1");
        add(contentArea);

        switchTab(0);
    }

    private Div buildHeader() {
        Div header = new Div();
        header.addClassName("app-header");

        H1 title = new H1("Payment Gateway EDA Demo");
        title.getStyle().set("font-size", "16px");
        title.getStyle().set("font-weight", "700");
        title.getStyle().set("margin", "0");

        Tabs tabs = new Tabs(
                new Tab("Payments"),
                new Tab("Saga States")
        );
        tabs.addSelectedChangeListener(e -> switchTab(tabs.getSelectedIndex()));

        Div nav = new Div();
        nav.addClassName("nav");
        nav.add(tabs);

        header.add(title, nav);
        return header;
    }

    private void switchTab(int index) {
        Component view = switch (index) {
            case 0 -> new PaymentsView(paymentClient, broadcaster, replayService, metricsService);
            case 1 -> new SagaView(sagaService);
            default -> new PaymentsView(paymentClient, broadcaster, replayService, metricsService);
        };
        contentArea.removeAll();
        contentArea.add(view);
    }
}
