package com.zaheudev.demo.dto;

import com.zaheudev.demo.model.Amount;
import com.zaheudev.demo.model.PaymentStatus;
import lombok.*;

import java.util.Date;

@Data @NoArgsConstructor @AllArgsConstructor
public class PaymentResponse {
    @Getter @Setter
    private String paymentId;
    @Getter @Setter
    private PaymentStatus paymentStatus;
    @Getter @Setter
    private Amount amount;
    @Getter @Setter
    private Date startedAt;
    @Getter @Setter
    private String message;

}
