package com.zaheudev.emulator.entity;

import com.zaheudev.emulator.model.TransactionStatus;
import com.zaheudev.shared.avro.PaymentMethodEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Data @AllArgsConstructor
@NoArgsConstructor @Builder
public class EmulatedRefundTransactionEntity {
    @Id
    private String refundId;
    private String paymentId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(referencedColumnName = "processorTransactionId")
    private EmulatedTransactionEntity originalProcessorTransactionId;
    private String refundRrn;
    private String refundAuthCode;
    private String errorMessage;
    private BigDecimal refundedAmount;
    private String currency;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;
}
