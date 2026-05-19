package com.zaheudev.emulator.entity;

import com.zaheudev.emulator.model.TransactionStatus;
import com.zaheudev.shared.avro.AuthorizationCompleted;
import com.zaheudev.shared.avro.PaymentMethodEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
@Entity @Builder
public class EmulatedTransactionEntity {
    private String paymentId;
    @Id
    private String processorTransactionId;
    private String rrn;
    private String authCode;
    @Enumerated(EnumType.STRING)
    private PaymentMethodEnum selectedPaymentMethod;
    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;
    private BigDecimal authorizedAmount;
    private BigDecimal capturedAmount;
    private BigDecimal refundedAmount;
    private String currency;
    private String errorMessage;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

}
