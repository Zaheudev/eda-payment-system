package com.zaheudev.gateway.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDetails {
    @Getter
    @Setter
    private String cardNumber;
    @Getter @Setter
    private String expiryMonth;
    @Getter @Setter
    private String expiryYear;
    @Getter @Setter
    private String cvv;
}
