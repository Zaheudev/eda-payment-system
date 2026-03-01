package com.zaheudev.demo.dto;

import com.zaheudev.demo.model.Amount;
import com.zaheudev.demo.model.Payment;
import com.zaheudev.demo.model.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

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
