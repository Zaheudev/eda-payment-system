package com.zaheudev.gateway.model;

import com.zaheudev.gateway.dto.CardDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    private String paymentId;
    private String merchantRef;
    private PaymentStatus status;
    private Amount amount;
    private CardDetails cardDetails;
    private String tokenRef;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Payment createPayment(String merchantRef, Amount amount, CardDetails cardDetails, String tokenRef){
        LocalDateTime now = LocalDateTime.now();
        return Payment.builder()
                .paymentId(generatePaymentId())
                .merchantRef(merchantRef)
                .status(PaymentStatus.CREATED)
                .amount(amount)
                .cardDetails(cardDetails)
                .createdAt(now)
                .updatedAt(now)
                .tokenRef(tokenRef)
                .build();
    }

    public static String generatePaymentId() {
        return "PMT" + UUID.randomUUID().toString().substring(0,9);
    }

    public void updateStatus(PaymentStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void authorize() {
        this.status = PaymentStatus.AUTHORIZED;
        this.updatedAt = LocalDateTime.now();
    }
}
