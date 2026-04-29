package com.zaheudev.shared.dto;

import lombok.AllArgsConstructor;import lombok.Builder;
import lombok.Data;import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DetokenizeResponse {
    private String cardNumber;
    private String expiryMonth;
    private String expiryYear;
}
