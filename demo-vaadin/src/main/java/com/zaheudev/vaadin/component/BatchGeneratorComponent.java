package com.zaheudev.vaadin.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.NumberField;
import com.zaheudev.vaadin.client.PaymentClient;
import com.zaheudev.vaadin.model.PaymentResponse;
import com.zaheudev.vaadin.util.CardGenerator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchGeneratorComponent extends VerticalLayout {

    private static final List<String> NETWORKS = List.of("VISA", "MASTERCARD", "AMEX", "DISCOVER");
    private static final List<String> CARD_TYPES = List.of("CREDIT", "DEBIT");
    private static final List<String> CURRENCIES = List.of("USD", "EUR", "RON");
    private static final int POOL_SIZE = 10;

    private final PaymentClient paymentClient;

    private final NumberField countField = new NumberField("Number of payments");
    private final Button sendBtn = new Button("Send Payments");
    private final Button cancelBtn = new Button("Cancel");
    private final ProgressBar progressBar = new ProgressBar();
    private final Span progressText = new Span();

    private Runnable onBatchComplete;
    private java.util.function.Consumer<String> onFirstPayment;

    private volatile boolean cancelled;
    private volatile ExecutorService batchExecutor;

    public BatchGeneratorComponent(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;

        addClassName("card");
        setWidthFull();
        setPadding(false);
        setSpacing(false);

        configureFields();
        layoutForm();
    }

    private void configureFields() {
        countField.setValue(20.0);
        countField.setMin(1);
        countField.setMax(50);
        countField.setStep(1);
        countField.setWidth("120px");

        sendBtn.addClassName("btn-primary");
        sendBtn.getStyle().set("font-size", "12px");
        sendBtn.addClickListener(e -> handleSend());

        cancelBtn.addClassName("btn-secondary");
        cancelBtn.getStyle().set("font-size", "12px");
        cancelBtn.setVisible(false);
        cancelBtn.addClickListener(e -> handleCancel());

        progressBar.setVisible(false);
        progressBar.setHeight("6px");
        progressText.getStyle().set("font-size", "12px").set("color", "var(--text-muted)");
    }

    private void layoutForm() {
        Div title = new Div();
        title.addClassName("card-title");
        title.setText("Batch Generator");

        HorizontalLayout row = new HorizontalLayout(countField, sendBtn, cancelBtn);
        row.setAlignItems(Alignment.END);
        row.setSpacing(true);

        Div progressWrapper = new Div();
        progressWrapper.addClassName("batch-progress");
        progressWrapper.add(progressBar, progressText);

        add(title, row, progressWrapper);
    }

    private void handleSend() {
        int n = countField.getValue() != null ? countField.getValue().intValue() : 20;
        if (n < 1) n = 1;
        if (n > 50) n = 50;
        final int total = n;

        cancelled = false;
        sendBtn.setEnabled(false);
        countField.setEnabled(false);
        cancelBtn.setVisible(true);

        progressBar.setValue(0);
        progressBar.setMax(n);
        progressBar.setVisible(true);
        progressText.setText("Sending 0 / " + n + " ...");

        AtomicInteger sent = new AtomicInteger(0);
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicBoolean firstTracked = new AtomicBoolean(false);

        UI ui = getUI().orElse(null);
        if (ui == null) return;

        batchExecutor = Executors.newFixedThreadPool(POOL_SIZE, r -> {
            Thread t = new Thread(r, "batch-sender");
            t.setDaemon(true);
            return t;
        });

        CompletableFuture<?>[] futures = new CompletableFuture[total];
        for (int i = 0; i < total; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                if (cancelled) return;
                try {
                    Map<String, Object> request = buildRandomRequest();
                    PaymentResponse res = paymentClient.createPayment(request);
                    int ok = succeeded.incrementAndGet();
                    if (res != null && res.paymentId() != null && firstTracked.compareAndSet(false, true)) {
                        String pid = res.paymentId();
                        ui.access(() -> {
                            if (onFirstPayment != null) onFirstPayment.accept(pid);
                        });
                    }
                } catch (Exception ex) {
                    failed.incrementAndGet();
                }
            }, batchExecutor).whenComplete((v, ex) -> {
                int done = sent.incrementAndGet();
                ui.access(() -> {
                    progressBar.setValue(done);
                    progressText.setText(String.format("Sent %d / %d | OK %d | Failed %d",
                            done, total, succeeded.get(), failed.get()));
                    if (done >= total) {
                        finishBatch(total, succeeded.get(), failed.get());
                    }
                });
            });
        }
    }

    private void finishBatch(int total, int ok, int fail) {
        progressText.setText(String.format("Done: %d sent | %d OK | %d failed", total, ok, fail));
        sendBtn.setEnabled(true);
        countField.setEnabled(true);
        cancelBtn.setVisible(false);
        if (onBatchComplete != null) onBatchComplete.run();
    }

    private void handleCancel() {
        cancelled = true;
        cancelBtn.setVisible(false);
        progressText.setText("Cancelling... remaining tasks will be skipped.");
    }

    private Map<String, Object> buildRandomRequest() {
        java.util.Random rng = new java.util.Random();
        String network = NETWORKS.get(rng.nextInt(NETWORKS.size()));
        String cardType = CARD_TYPES.get(rng.nextInt(CARD_TYPES.size()));
        String currency = CURRENCIES.get(rng.nextInt(CURRENCIES.size()));
        int amount = rng.nextInt(491) + 10;

        Map<String, Object> cardDetails = new LinkedHashMap<>();
        cardDetails.put("cardNumber", CardGenerator.generatePAN(network, cardType));
        cardDetails.put("cvv", CardGenerator.generateCvv());
        String[] exp = CardGenerator.generateExpiry().split("/");
        cardDetails.put("expiryMonth", exp[0]);
        cardDetails.put("expiryYear", exp[1]);
        cardDetails.put("cardHolderName", CardGenerator.generateCardholder());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("merchantReference", CardGenerator.generateMerchantRef());
        request.put("amount", amount);
        request.put("currency", currency);
        request.put("cardDetails", cardDetails);
        return request;
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        cancelled = true;
        if (batchExecutor != null && !batchExecutor.isTerminated()) {
            batchExecutor.shutdownNow();
            try {
                batchExecutor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        super.onDetach(detachEvent);
    }

    public void setOnBatchComplete(Runnable onBatchComplete) {
        this.onBatchComplete = onBatchComplete;
    }

    public void setOnFirstPayment(java.util.function.Consumer<String> onFirstPayment) {
        this.onFirstPayment = onFirstPayment;
    }
}
