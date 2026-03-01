package com.zaheudev.demo.entity;

import com.zaheudev.demo.model.Payment;
import com.zaheudev.demo.model.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table(name = "payments")
@Data @Builder
@Entity @NoArgsConstructor @AllArgsConstructor
public class PaymentEntity {
    @Id @Getter
    private String paymentId;
    @Getter
    private PaymentStatus status;
    @Getter
    private String tokenRef;
    @Getter
    private LocalDateTime createdAt;
    @Getter
    private LocalDateTime updatedAt;
    @Getter
    private BigDecimal amount;
    @Getter
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
                .build();
    }
}
