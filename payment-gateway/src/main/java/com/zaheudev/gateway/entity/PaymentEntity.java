package com.zaheudev.gateway.entity;

import com.zaheudev.gateway.dto.PaymentResponse;
import com.zaheudev.gateway.model.Amount;
import com.zaheudev.gateway.model.Payment;
import com.zaheudev.shared.dto.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table(name = "payments")
@Data @Builder
@Entity @NoArgsConstructor
@AllArgsConstructor
public class PaymentEntity {
    @Id
    private String paymentId;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String rrn;
    private String authCode;
    private String errorMessage;
    private String processorTransactionId;
    private String captureId;
    private String merchantRef;
    private String tokenRef;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private String currency;

    public static PaymentEntity fromPayment(Payment payment){
        return PaymentEntity.builder()
                .paymentId(payment.getPaymentId())
                .status(payment.getStatus())
                .tokenRef(payment.getTokenRef())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .amount(BigDecimal.valueOf(payment.getAmount().getAmount()).divide(BigDecimal.valueOf(100)))
                .currency(payment.getAmount().getCurrency())
                .tokenRef(payment.getTokenRef())
                .build();
    }

    public PaymentResponse tranformInPaymentResponse(String message){
        return PaymentResponse.builder()
                .paymentId(this.paymentId)
                .paymentStatus(this.status)
                .captureId(this.captureId)
                .rrn(this.rrn)
                .authCode(this.authCode)
                .processorTransactionId(this.processorTransactionId)
                .amount(Amount.builder()
                        .amount(this.amount.multiply(BigDecimal.valueOf(100)).longValue())
                        .currency(this.currency)
                        .build())
                .createdAt(this.createdAt)
                .paymentStatus(this.status)
                .message(this.errorMessage != null ? this.errorMessage : message)
                .build();
    }
}
