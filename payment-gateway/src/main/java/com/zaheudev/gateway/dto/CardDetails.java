package com.zaheudev.gateway.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDetails {
    private String cardNumber;
    private String expiryMonth;
    private String expiryYear;
    private String cvv;
}
