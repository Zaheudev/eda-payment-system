package com.zaheudev.vaadin.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.zaheudev.vaadin.client.PaymentClient;
import com.zaheudev.vaadin.model.PaymentResponse;
import com.zaheudev.vaadin.util.CardGenerator;

import java.util.LinkedHashMap;
import java.util.Map;

public class PaymentFormComponent extends VerticalLayout {

    private final PaymentClient paymentClient;

    private final TextField merchantRef = new TextField("Merchant Ref");
    private final NumberField amount = new NumberField("Amount");
    private final Select<String> currency = new Select<>();
    private final TextField cardNumber = new TextField("Card Number");
    private final TextField cvv = new TextField("CVV");
    private final TextField expiry = new TextField("Expiry (MM/YYYY)");
    private final TextField cardholder = new TextField("Cardholder");

    private final Select<String> genNetwork = new Select<>();
    private final Select<String> genCardType = new Select<>();
    private final Button genBtn = new Button("Generate Card");

    private final Button createBtn = new Button("Create Payment");
    private final Button captureBtn = new Button("Capture");
    private final Button voidBtn = new Button("Void");
    private final Button refundBtn = new Button("Refund");

    private final Paragraph message = new Paragraph();
    private final Paragraph lastPidDisplay = new Paragraph();

    private String lastPid = "";
    private Runnable onSuccess;
    private java.util.function.Consumer<String> onTrack;

    public PaymentFormComponent(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;

        addClassName("card");
        configureFields();
        layoutForm();
        configureButtons();
    }

    private void configureFields() {
        merchantRef.setValue("order-" + Long.toString(System.currentTimeMillis(), 36));
        amount.setValue(99.99);
        amount.setStepButtonsVisible(false);

        currency.setLabel("Currency");
        currency.setItems("USD", "EUR", "RON");
        currency.setValue("USD");

        cardNumber.setValue("4111111111111111");
        cvv.setValue("123");
        expiry.setValue("12/2027");
        cardholder.setValue("John Doe");

        genNetwork.setLabel("Network");
        genNetwork.setItems("VISA", "MASTERCARD", "AMEX", "DISCOVER");
        genNetwork.setValue("VISA");

        genCardType.setLabel("Card Type");
        genCardType.setItems("CREDIT", "DEBIT");
        genCardType.setValue("CREDIT");

        genBtn.addClassName("btn-secondary");
        genBtn.getStyle().set("font-size", "12px");

        merchantRef.getStyle().set("--vaadin-field-default-width", "100%");
        amount.getStyle().set("--vaadin-field-default-width", "100%");
        currency.getStyle().set("--vaadin-field-default-width", "100%");
        cardNumber.getStyle().set("--vaadin-field-default-width", "100%");
        cvv.getStyle().set("--vaadin-field-default-width", "100%");
        expiry.getStyle().set("--vaadin-field-default-width", "100%");
        cardholder.getStyle().set("--vaadin-field-default-width", "100%");
    }

    private void layoutForm() {
        Div title = new Div();
        title.addClassName("card-title");
        title.setText("Create Payment");

        genNetwork.setWidthFull();
        genNetwork.setMinWidth("200px");
        genCardType.setWidthFull();
        genCardType.setMinWidth("180px");

        HorizontalLayout row1 = new HorizontalLayout(merchantRef, amount, currency);
        row1.addClassName("form-row");
        row1.setWidthFull();

        HorizontalLayout row2 = new HorizontalLayout(cardNumber, cvv, expiry, cardholder);
        row2.addClassName("form-row");
        row2.setWidthFull();

        Div genTitle = new Div();
        genTitle.addClassName("card-title");
        genTitle.setText("Generate Card");
        genTitle.getStyle().set("margin-top", "12px");

        HorizontalLayout genRow = new HorizontalLayout(genNetwork, genCardType, genBtn);
        genRow.addClassName("form-row");
        genRow.setWidthFull();
        genRow.setAlignItems(Alignment.END);

        message.getStyle().set("font-size", "12px").set("color", "var(--text-muted)");
        lastPidDisplay.getStyle().set("font-size", "12px").set("font-family", "var(--mono)");

        add(title, row1, genTitle, genRow, row2, createButtons(), message, lastPidDisplay);
        setPadding(false);
        setSpacing(false);
    }

    private HorizontalLayout createButtons() {
        HorizontalLayout btns = new HorizontalLayout(createBtn, captureBtn, voidBtn, refundBtn);
        btns.setSpacing(true);
        btns.getStyle().set("margin-top", "8px");
        return btns;
    }

    private void configureButtons() {
        createBtn.addClassName("btn-primary");
        createBtn.addClickListener(e -> handleCreate());
        captureBtn.addClassName("btn-secondary");
        voidBtn.addClassName("btn-secondary");
        refundBtn.addClassName("btn-secondary");

        captureBtn.setEnabled(false);
        voidBtn.setEnabled(false);
        refundBtn.setEnabled(false);

        captureBtn.addClickListener(e -> handleCapture());
        voidBtn.addClickListener(e -> handleVoid());
        refundBtn.addClickListener(e -> handleRefund());

        genBtn.addClickListener(e -> handleGenerate());
    }

    private void handleGenerate() {
        String network = genNetwork.getValue();
        String cardType = genCardType.getValue();
        cardNumber.setValue(CardGenerator.generatePAN(network, cardType));
        cvv.setValue(CardGenerator.generateCvv());
        expiry.setValue(CardGenerator.generateExpiry());
        cardholder.setValue(CardGenerator.generateCardholder());
        merchantRef.setValue(CardGenerator.generateMerchantRef());
        message.setText("Generated " + network + " " + cardType + " card");
    }

    private void handleCreate() {
        message.setText("Sending...");

        Map<String, Object> cardDetails = new LinkedHashMap<>();
        cardDetails.put("cardNumber", cardNumber.getValue());
        cardDetails.put("cvv", cvv.getValue());
        String[] exp = expiry.getValue().split("/");
        if (exp.length >= 2) {
            cardDetails.put("expiryMonth", exp[0].trim());
            cardDetails.put("expiryYear", exp[1].trim());
        }
        cardDetails.put("cardHolderName", cardholder.getValue());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("merchantReference", merchantRef.getValue());
        request.put("amount", amount.getValue().intValue());
        request.put("currency", currency.getValue());
        request.put("cardDetails", cardDetails);

        try {
            PaymentResponse res = paymentClient.createPayment(request);
            lastPid = res.paymentId() != null ? res.paymentId() : "";
            message.setText("Created: " + lastPid);
            merchantRef.setValue("order-" + Long.toString(System.currentTimeMillis(), 36));
            amount.setValue((double) (int) (Math.random() * 500 + 10));

            captureBtn.setEnabled(true);
            voidBtn.setEnabled(true);
            refundBtn.setEnabled(true);
            updateLastPidDisplay();

            if (onSuccess != null) onSuccess.run();
            if (onTrack != null && !lastPid.isEmpty()) onTrack.accept(lastPid);
        } catch (Exception ex) {
            message.setText("Error: " + ex.getMessage());
        }
    }

    private void handleCapture() {
        if (lastPid.isEmpty()) return;
        message.setText("Capturing...");
        try {
            paymentClient.capture(lastPid);
            message.setText("Capture sent for " + lastPid);
            if (onSuccess != null) onSuccess.run();
        } catch (Exception ex) {
            message.setText("Error: " + ex.getMessage());
        }
    }

    private void handleVoid() {
        if (lastPid.isEmpty()) return;
        message.setText("Voiding...");
        try {
            paymentClient.voidPayment(lastPid);
            message.setText("Void sent for " + lastPid);
            if (onSuccess != null) onSuccess.run();
        } catch (Exception ex) {
            message.setText("Error: " + ex.getMessage());
        }
    }

    private void handleRefund() {
        if (lastPid.isEmpty()) return;
        message.setText("Refunding...");
        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("amount", amount.getValue().intValue());
            req.put("currency", currency.getValue());
            paymentClient.refund(lastPid, req);
            message.setText("Refund sent for " + lastPid);
            if (onSuccess != null) onSuccess.run();
        } catch (Exception ex) {
            message.setText("Error: " + ex.getMessage());
        }
    }

    private void updateLastPidDisplay() {
        lastPidDisplay.getElement().setProperty("innerHTML",
                "Last: <strong>" + lastPid + "</strong>");
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void setOnTrack(java.util.function.Consumer<String> onTrack) {
        this.onTrack = onTrack;
    }

    public String getLastPid() {
        return lastPid;
    }
}
