package com.zaheudev.gateway.dto;

import com.zaheudev.gateway.model.Amount;
import com.zaheudev.gateway.model.Payment;
import com.zaheudev.shared.dto.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
@Builder
public class PaymentResponse {
    private String paymentId;
    private PaymentStatus paymentStatus;
    private String rrn;
    private String authCode;
    private String processorTransactionId;
    private String captureId;
    private Amount amount;
    private LocalDateTime createdAt;
    private String message;

    public static PaymentResponse create(Payment payment){
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentStatus(payment.getStatus())
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .message("Payment request is created")
                .build();
    }

}
