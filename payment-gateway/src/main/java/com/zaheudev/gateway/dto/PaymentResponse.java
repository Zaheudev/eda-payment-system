package com.zaheudev.gateway.dto;

import com.zaheudev.gateway.model.Amount;
import com.zaheudev.gateway.model.Payment;
import com.zaheudev.gateway.model.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
@Builder
public class PaymentResponse {
    @Getter @Setter
    private String paymentId;
    @Getter @Setter
    private PaymentStatus paymentStatus;
    @Getter @Setter
    private Amount amount;
    @Getter @Setter
    private LocalDateTime createdAt;
    @Getter @Setter
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
