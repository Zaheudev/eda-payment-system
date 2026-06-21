package com.zaheudev.vaadin.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.zaheudev.vaadin.client.PaymentClient;

import java.math.BigDecimal;

public class TotalFees extends VerticalLayout {
    private final PaymentClient paymentClient;
    private BigDecimal firstTotalFees;
    private BigDecimal secondTotalFees;

    private final Div content =  new Div();
    private String errorMessage = null;

    public TotalFees(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
        fetchTotalCosts();
        buildDetails();
        add(content);
    }

    public void fetchTotalCosts(){
        try{
            BigDecimal[] costs = paymentClient.fetchFeeCosts();
            firstTotalFees = costs[0];
            secondTotalFees = costs[1];
        }catch (Exception e){
            errorMessage = "Failed to load total fees: " + e.getMessage();
        }
    }

    public void buildDetails(){
        StringBuilder sb = new StringBuilder();
        sb.append("First option total fees: ").append(firstTotalFees).append("\n").append("Second option total fees: ").append(secondTotalFees);
        content.setText(errorMessage == null ? sb.toString() : errorMessage);
    }

}
