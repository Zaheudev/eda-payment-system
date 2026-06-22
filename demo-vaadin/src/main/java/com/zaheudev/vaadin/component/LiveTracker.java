package com.zaheudev.vaadin.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.zaheudev.vaadin.client.PaymentClient;
import com.zaheudev.vaadin.model.EventEnvelope;
import com.zaheudev.vaadin.model.PaymentResponse;
import com.zaheudev.vaadin.service.EventBroadcaster;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LiveTracker extends VerticalLayout {

    private static final List<String> ORDER = List.of(
            "CREATED", "RISK_ASSESSED", "ROUTING_COMPLETED", "AUTHORIZED",
            "CAPTURED", "VOID", "REFUNDED", "PARTIALLY_REFUNDED"
    );
    private static final Set<String> TERMINAL = Set.of("REJECTED", "FAILED", "VOID", "REFUNDED");
    private static final List<String> ALL_NODES = List.of(
            "CREATED", "RISK_ASSESSED", "ROUTING_COMPLETED", "AUTHORIZED",
            "CAPTURED", "VOID", "REFUNDED", "PARTIALLY_REFUNDED", "REJECTED", "FAILED"
    );

    private final PaymentClient paymentClient;
    private final EventBroadcaster broadcaster;
    private final com.zaheudev.vaadin.service.PaymentEventReplayService replayService;

    private final TextField searchInput = new TextField();
    private final Button trackBtn = new Button("Track");
    private final Button stopBtn = new Button("Stop Tracking");
    private final TextField refundAmountField = new TextField("Refund amount");

    private final Div trackerContent = new Div();

    private String paymentId;
    private final Set<String> seenKeys = new HashSet<>();
    private final List<TimelineNode> timeline = new ArrayList<>();
    private final Set<String> reachedStates = new LinkedHashSet<>();
    private boolean terminal;
    private Long tStart;
    private Long tEnd;
    private PaymentResponse paymentData;
    private boolean showRaw;
    private String errorMessage;
    private String actionError;
    private long refundedAmountCents;
    private String refundedCurrency;
    private final java.util.concurrent.atomic.AtomicBoolean refreshPending =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private final ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "tracker-tick");
        t.setDaemon(true);
        return t;
    });

    private Runnable onStop;
    private java.util.function.Consumer<String> onTrack;

    public LiveTracker(PaymentClient paymentClient, EventBroadcaster broadcaster,
                       com.zaheudev.vaadin.service.PaymentEventReplayService replayService) {
        this.paymentClient = paymentClient;
        this.broadcaster = broadcaster;
        this.replayService = replayService;

        addClassName("card");
        configureSearch();
        layoutSearch();
        setPadding(false);
        setSpacing(false);
    }

    private void configureSearch() {
        searchInput.setPlaceholder("Paste payment ID to track...");
        searchInput.setWidthFull();
        trackBtn.addClassName("btn-primary");
        trackBtn.getStyle().set("font-size", "12px");
        stopBtn.addClassName("btn-secondary");
        stopBtn.getStyle().set("font-size", "12px");
        stopBtn.setVisible(false);

        refundAmountField.setPlaceholder("0.00");
        refundAmountField.setWidth("120px");
        refundAmountField.getStyle().set("font-size", "12px");
        refundAmountField.setVisible(false);

        trackBtn.addClickListener(e -> {
            String val = searchInput.getValue().trim();
            if (!val.isEmpty()) {
                startTracking(val);
            }
        });
        stopBtn.addClickListener(e -> stopTracking());
    }

    private void layoutSearch() {
        HorizontalLayout searchRow = new HorizontalLayout(searchInput, trackBtn, stopBtn);
        searchRow.addClassName("tracker-search");
        searchRow.setWidthFull();
        searchRow.setAlignItems(Alignment.CENTER);
        add(searchRow, trackerContent);
    }

    public void startTracking(String pid) {
        reset();
        this.paymentId = pid;
        stopBtn.setVisible(true);
        try {
            PaymentResponse data = paymentClient.fetchPayment(pid);
            if (data != null) {
                this.paymentData = data;
            }
        } catch (HttpClientErrorException.NotFound e) {
            errorMessage = "Payment not found: " + pid;
        } catch (Exception e) {
            errorMessage = "Failed to load payment: " + e.getMessage();
        }

        if (errorMessage == null) {
            for (EventEnvelope ev : broadcaster.getRecentEvents()) {
                ingest(ev);
            }
            replayService.replayForPayment(pid).thenAccept(events ->
                    getUI().ifPresent(ui -> ui.access(() -> {
                        for (EventEnvelope ev : events) {
                            ingest(ev);
                        }
                        renderTracker();
                    })));
        }

        if (onTrack != null) {onTrack.accept(pid);}
        renderTracker();
    }

    public void stopTracking() {
        reset();
        renderTracker();
        if (onStop != null) onStop.run();
    }

    private void reset() {
        paymentId = null;
        seenKeys.clear();
        timeline.clear();
        reachedStates.clear();
        terminal = false;
        tStart = null;
        tEnd = null;
        paymentData = null;
        showRaw = false;
        errorMessage = null;
        actionError = null;
        refundedAmountCents = 0;
        refundedCurrency = null;
        stopBtn.setVisible(false);
    }

    private boolean ingest(EventEnvelope event) {
        if (paymentId == null || !paymentId.equals(event.getPaymentId())) return false;
        String key = event.getTopic() + ":" + event.getPartition() + ":" + event.getOffset();
        if (seenKeys.contains(key)) return false;
        seenKeys.add(key);

        String state = eventToState(event);
        if (state == null) return false;

        if ("refund-completed".equals(event.getTopic())) {
            captureRefundedAmount(asMap(event.getPayload()));
        }

        reachedStates.add(state);
        timeline.add(new TimelineNode(state, event.getTimestamp(), shortSummary(event)));

        if (tStart == null) {
            tStart = event.getTimestamp();
        }

        if ((state.equals("AUTHORIZED") || TERMINAL.contains(state)) && tEnd == null) {
            tEnd = event.getTimestamp();
        }

        if (TERMINAL.contains(state)) {
            terminal = true;
        }
        return true;
    }

    public void onEvent(EventEnvelope event) {
        if (paymentId == null || terminal || errorMessage != null) return;
        if (!ingest(event)) return;

        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                PaymentResponse data = paymentClient.fetchPayment(paymentId);
                if (data != null) this.paymentData = data;
            } catch (Exception ignored) {
            }
            renderTracker();
        }));
        scheduleStateRefresh();
    }

    private String eventToState(EventEnvelope e) {
        var p = asMap(e.getPayload());
        return switch (e.getTopic()) {
            case "payment-requests" -> "CREATED";
            case "risk-assessed" -> "RISK_ASSESSED";
            case "payment-rejected" -> "REJECTED";
            case "routing-completed" -> "ROUTING_COMPLETED";
            case "authorization-completed" -> bool(p, "success") ? "AUTHORIZED" : "FAILED";
            case "capture-completed" -> bool(p, "success") ? "CAPTURED" : "FAILED";
            case "void-completed" -> bool(p, "success") ? "VOID" : "FAILED";
            case "refund-completed" -> "REFUNDED".equals(str(p, "status")) ? "REFUNDED" : "PARTIALLY_REFUNDED";
            default -> null;
        };
    }

    private String shortSummary(EventEnvelope e) {
        var p = asMap(e.getPayload());
        return switch (e.getTopic()) {
            case "payment-requests" -> "Payment created";
            case "risk-assessed" -> "Risk: " + str(p, "riskLevel", "?");
            case "payment-rejected" -> "Rejected: " + str(p, "reason", "");
            case "routing-completed" -> "Network: " + str(p, "selectedPaymentMethod", "?")
                    + " cost=" + str(p, "estimatedCost", "?");
            case "authorization-completed" -> bool(p, "success")
                    ? "RRN: " + str(p, "rrn", "") + " auth: " + str(p, "authCode", "")
                    : "Failed: " + str(p, "errorMessage", "");
            case "capture-completed" -> bool(p, "success")
                    ? "Capture: " + str(p, "captureId", "")
                    : "Capture failed: " + str(p, "errorMessage", "");
            case "void-completed" -> bool(p, "success") ? "Voided" : "Void failed: " + str(p, "errorMessage", "");
            case "refund-completed" -> "Refund: " + str(p, "status", "");
            default -> "";
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object payload) {
        if (payload instanceof Map) return (Map<String, Object>) payload;
        return Map.of();
    }

    private String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? String.valueOf(v) : def;
    }

    private String str(Map<String, Object> m, String key) {
        return str(m, key, "");
    }

    private boolean bool(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Boolean b ? b : false;
    }

    @SuppressWarnings("unchecked")
    private void captureRefundedAmount(Map<String, Object> p) {
        Object ra = p.get("refundedAmount");
        if (ra instanceof Map<?, ?> m) {
            Object v = m.get("value");
            Object c = m.get("currency");
            if (v instanceof Number n) refundedAmountCents = n.longValue();
            if (c != null) refundedCurrency = String.valueOf(c);
        }
    }

    private int stateIndex(String state) {
        int i = ORDER.indexOf(state);
        return i >= 0 ? i : ORDER.size();
    }

    private void renderTracker() {
        Div content = new Div();
        content.setWidthFull();

        if (paymentId == null) {
            Paragraph p = new Paragraph("Enter a payment ID above or create a payment to start tracking.");
            p.getStyle().set("color", "var(--text-muted)").set("font-size", "13px");
            content.add(p);
            trackerContent.removeAll();
            trackerContent.add(content);
            return;
        }

        if (errorMessage != null) {
            Paragraph err = new Paragraph(errorMessage);
            err.getStyle().set("color", "var(--danger)").set("font-size", "13px");
            content.add(err);
            trackerContent.removeAll();
            trackerContent.add(content);
            return;
        }

        Div header = buildHeader();
        content.add(header);

        Div timelineDiv = buildTimeline();
        timelineDiv.getStyle().set("margin-top", "12px");
        content.add(timelineDiv);

        if (paymentData != null) {
            content.add(buildPaymentDetails());
        }

        if (timeline.isEmpty()) {
            Paragraph p = new Paragraph("Waiting for events. The detail grid shows snapshot data; the timeline populates as events arrive.");
            p.getStyle().set("margin-top", "8px").set("font-size", "12px").set("color", "var(--text-muted)");
            content.add(p);
        }

        content.add(buildActions());

        if (!timeline.isEmpty()) {
            content.add(buildRawEvents());
        }

        trackerContent.removeAll();
        trackerContent.add(content);
    }

    private Div buildHeader() {
        Div header = new Div();
        header.addClassName("tracker-header");

        HorizontalLayout left = new HorizontalLayout();
        left.setSpacing(true);
        left.setAlignItems(Alignment.CENTER);

        Span pidSpan = new Span(paymentId.length() > 12 ? paymentId.substring(0, 12) + "..." : paymentId);
        pidSpan.addClassName("tracker-pid");
        pidSpan.setTitle(paymentId);
        left.add(pidSpan);

        if (paymentData != null) {
            String status = !timeline.isEmpty() ? timeline.get(timeline.size() - 1).state()
                    : paymentData.paymentStatus();
            Span statusBadge = new Span(status);
            statusBadge.addClassName("badge");
            statusBadge.addClassName(statusClass(status));
            left.add(statusBadge);
        }

        String elapsedText = computeElapsed();
        if (elapsedText != null) {
            Span elapsed = new Span(elapsedText);
            elapsed.addClassName("tracker-elapsed");
            if (terminal) {
                elapsed.addClassName(reachedStates.contains("REJECTED") || reachedStates.contains("FAILED")
                        ? "tracker-elapsed--failed" : "tracker-elapsed--done");
            } else {
                elapsed.addClassName("tracker-elapsed--running");
            }
            left.add(elapsed);
        }

        header.add(left, stopBtn);
        return header;
    }

    private Div buildTimeline() {
        Div container = new Div();
        container.addClassName("timeline");

        for (String state : ALL_NODES) {
            TimelineNode node = timeline.stream()
                    .filter(n -> n.state.equals(state))
                    .findFirst().orElse(null);

            Div row = new Div();
            row.addClassName("timeline-row");

            Div nodeDiv = new Div();
            nodeDiv.setText(state);
            nodeDiv.addClassName("timeline-node");
            nodeDiv.addClassName(getNodeClass(state));

            Div meta = new Div();
            meta.addClassName("timeline-meta");

            if (node != null) {
                Span time = new Span(formatTs(node.timestamp));
                time.addClassName("timeline-time");
                Span detail = new Span(node.detail);
                detail.addClassName("timeline-detail");
                meta.add(time, detail);
            }

            row.add(nodeDiv, meta);
            container.add(row);
        }

        return container;
    }

    private String getNodeClass(String state) {
        if ("REJECTED".equals(state) || "FAILED".equals(state)) return "timeline-node--failed";
        if (reachedStates.contains(state)) return "timeline-node--done";
        int highest = reachedStates.stream().mapToInt(this::stateIndex).max().orElse(-1);
        if (stateIndex(state) == highest + 1 && highest < ORDER.size()) return "timeline-node--active";
        return "timeline-node--pending";
    }

    private Div buildPaymentDetails() {
        Div details = new Div();
        details.getStyle().set("margin-top", "16px");

        Div title = new Div();
        title.addClassName("card-title");
        title.setText("Payment Details");
        details.add(title);

        Div kvGrid = new Div();
        kvGrid.addClassName("kv-grid");

        PaymentResponse.Amount amt = paymentData.amount();
        String amountStr = amt != null
                ? String.format("%.2f %s", amt.amount() / 100.0, amt.currency())
                : "-";

        String currency = amt != null ? amt.currency() : "";
        long eventCents = refundedAmountCents;
        long snapCents = paymentData.refundedAmount() != null
                ? paymentData.refundedAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue() : 0;
        long refundedCents = Math.max(eventCents, snapCents);
        String refundedAmountStr = refundedCents > 0
                ? String.format("%.2f %s", refundedCents / 100.0, currency)
                : "-";

        String[][] rows = {
                {"Payment ID", paymentData.paymentId()},
                {"Amount", amountStr},
                {"Refunded amount", refundedAmountStr},
                {"Status", paymentData.paymentStatus()},
                {"RRN", nvl(paymentData.rrn())},
                {"Auth Code", nvl(paymentData.authCode())},
                {"Processor TXN ID", nvl(paymentData.processorTransactionId())},
                {"Capture ID", nvl(paymentData.captureId())},
                {"Error", paymentData.message() != null
                        ? "<span style='color:var(--danger)'>" + paymentData.message() + "</span>" : "-"},
                {"Created", paymentData.createdAt() != null
                        ? formatTs(parseToEpochMilli(paymentData.createdAt())) : "-"},
        };

        for (String[] row : rows) {
            Span k = new Span();
            k.addClassName("k");
            k.setText(row[0]);
            Span v = new Span();
            v.addClassName("v");
            v.getElement().setProperty("innerHTML", row[1]);
            kvGrid.add(k, v);
        }

        details.add(kvGrid);
        return details;
    }

    private Div buildActions() {
        Div actions = new Div();
        actions.addClassName("tracker-actions");

        boolean canCapture = paymentData != null && "AUTHORIZED".equals(paymentData.paymentStatus());
        boolean canVoid = paymentData != null && "AUTHORIZED".equals(paymentData.paymentStatus());
        boolean canRefund = paymentData != null
                && ("CAPTURED".equals(paymentData.paymentStatus()) || "PARTIALLY_REFUNDED".equals(paymentData.paymentStatus()));

        Button captureAct = new Button("Capture");
        captureAct.addClassName("btn-primary");
        captureAct.getStyle().set("font-size", "12px");
        captureAct.setEnabled(canCapture);
        captureAct.addClickListener(e -> doCapture());

        Button voidAct = new Button("Void");
        voidAct.addClassName("btn-secondary");
        voidAct.getStyle().set("font-size", "12px");
        voidAct.setEnabled(canVoid);
        voidAct.addClickListener(e -> doVoid());

        Button refundAct = new Button("Refund");
        refundAct.addClassName("btn-secondary");
        refundAct.getStyle().set("font-size", "12px");
        refundAct.setEnabled(canRefund);
        refundAct.addClickListener(e -> doRefund());

        double capturedUnits = paymentData != null && paymentData.amount() != null
                ? paymentData.amount().amount() / 100.0 : 0;
        refundAmountField.setLabel("Refund amount (" + (paymentData != null && paymentData.amount() != null
                ? paymentData.amount().currency() : "") + ")");
        String defaultVal = String.format("%.2f", capturedUnits);
        if (refundAmountField.getValue() == null || refundAmountField.getValue().isBlank()) {
            refundAmountField.setValue(defaultVal);
        } else {
            try {
                double cur = Double.parseDouble(refundAmountField.getValue().trim());
                if (cur > capturedUnits || cur <= 0) refundAmountField.setValue(defaultVal);
            } catch (NumberFormatException ex) {
                refundAmountField.setValue(defaultVal);
            }
        }
        refundAmountField.setEnabled(canRefund);
        refundAmountField.setVisible(canRefund);

        actions.add(captureAct, voidAct, refundAmountField, refundAct);

        if (actionError != null) {
            Paragraph err = new Paragraph(actionError);
            err.getStyle().set("color", "var(--danger)").set("font-size", "12px").set("margin-top", "8px");
            actions.add(err);
        }
        return actions;
    }

    private void doCapture() {
        if (paymentId == null) return;
        actionError = null;
        try {
            paymentClient.capture(paymentId);
            refreshPaymentData();
            scheduleStateRefresh();
        } catch (Exception e) {
            actionError = "Capture failed: " + rootMessage(e);
        }
        renderTracker();
    }

    private void doVoid() {
        if (paymentId == null) return;
        actionError = null;
        try {
            paymentClient.voidPayment(paymentId);
            refreshPaymentData();
            scheduleStateRefresh();
        } catch (Exception e) {
            actionError = "Void failed: " + rootMessage(e);
        }
        renderTracker();
    }

    private void doRefund() {
        if (paymentId == null) return;
        actionError = null;
        double amt;
        try {
            amt = Double.parseDouble(refundAmountField.getValue().trim());
        } catch (Exception ex) {
            actionError = "Enter a valid refund amount";
            renderTracker();
            return;
        }
        if (amt <= 0) {
            actionError = "Enter a refund amount greater than 0";
            renderTracker();
            return;
        }
        double capturedUnits = paymentData != null && paymentData.amount() != null
                ? paymentData.amount().amount() / 100.0 : 0;
        if (amt > capturedUnits) {
            actionError = "Refund amount exceeds captured amount (" + String.format("%.2f", capturedUnits) + ")";
            renderTracker();
            return;
        }
        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("amount", amt);
            req.put("currency", paymentData != null && paymentData.amount() != null
                    ? paymentData.amount().currency() : "USD");
            paymentClient.refund(paymentId, req);
            refreshPaymentData();
            scheduleStateRefresh();
        } catch (Exception e) {
            actionError = "Refund failed: " + rootMessage(e);
        }
        renderTracker();
    }

    private void scheduleStateRefresh() {
        if (!refreshPending.compareAndSet(false, true)) return;
        String pid = this.paymentId;
        long[] delays = { 600, 1800, 3500 };
        for (long d : delays) {
            ticker.schedule(() -> getUI().ifPresent(ui -> ui.access(() -> {
                if (pid == null || !pid.equals(this.paymentId)) return;
                refreshPaymentData();
                renderTracker();
            })), d, TimeUnit.MILLISECONDS);
        }
        ticker.schedule(() -> refreshPending.set(false), 3600, TimeUnit.MILLISECONDS);
    }

    private void refreshPaymentData() {
        try {
            PaymentResponse data = paymentClient.fetchPayment(paymentId);
            if (data != null) this.paymentData = data;
        } catch (Exception ignored) {
        }
    }

    private static String rootMessage(Exception e) {
        Throwable c = e;
        while (c.getCause() != null) c = c.getCause();
        return c.getMessage();
    }

    private Div buildRawEvents() {
        Div raw = new Div();
        raw.getStyle().set("margin-top", "16px");

        Button toggle = new Button(showRaw ? "Hide Raw Events (" + timeline.size() + ")" : "Show Raw Events (" + timeline.size() + ")");
        toggle.addClassName("btn-secondary");
        toggle.getStyle().set("font-size", "12px");
        toggle.addClickListener(e -> {
            showRaw = !showRaw;
            renderTracker();
        });
        raw.add(toggle);

        if (showRaw) {
            Div tape = new Div();
            tape.addClassName("tape");
            tape.getStyle().set("margin-top", "8px");
            for (TimelineNode n : timeline) {
                Div line = new Div();
                line.addClassName("tape-line");
                Span topic = new Span(n.state);
                topic.addClassName("topic");
                Span id = new Span(" " + formatTs(n.timestamp) + " ");
                id.addClassName("id");
                Span payload = new Span(n.detail);
                payload.addClassName("payload");
                line.add(topic, id, payload);
                tape.add(line);
            }
            raw.add(tape);
        }

        return raw;
    }

    private String computeElapsed() {
        if (tStart == null) return null;
        long end = tEnd != null ? tEnd : System.currentTimeMillis();
        long ms = end - tStart;
        if (tEnd != null && (reachedStates.contains("REJECTED") || reachedStates.contains("FAILED"))) {
            return "Failed after: " + fmtMs(ms);
        }
        if (tEnd != null && reachedStates.contains("AUTHORIZED")) {
            return "Total: " + fmtMs(ms);
        }
        if (tEnd != null) {
            return "Total: " + fmtMs(ms);
        }
        return "Elapsed: " + fmtMs(ms);
    }

    private String statusClass(String status) {
        return switch (status) {
            case "AUTHORIZED", "CAPTURED", "REFUNDED" -> "badge-success";
            case "REJECTED", "FAILED" -> "badge-danger";
            case "VOID" -> "badge-warning";
            default -> "badge-info";
        };
    }

    private static long parseToEpochMilli(String s) {
        try {
            return Instant.parse(s).toEpochMilli();
        } catch (DateTimeParseException e) {
            return java.time.LocalDateTime.parse(s)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
    }

    private static String formatTs(long timestamp) {
        LocalTime time = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalTime();
        return time.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "."
                + String.format("%03d", timestamp % 1000);
    }

    private static String fmtMs(long ms) {
        if (ms < 1000) return ms + " ms";
        return String.format("%.2f s", ms / 1000.0);
    }

    private static String nvl(String s) {
        return s != null ? s : "-";
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        broadcaster.subscribe(ui, this::onEvent);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcaster.unsubscribe(detachEvent.getUI());
        super.onDetach(detachEvent);
    }

    public void setOnStop(Runnable onStop) {
        this.onStop = onStop;
    }

    public void setOnTrack(java.util.function.Consumer<String> onTrack) {
        this.onTrack = onTrack;
    }

    private record TimelineNode(String state, long timestamp, String detail) {}
}
