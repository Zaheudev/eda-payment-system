package com.zaheudev.shared.dto;

import lombok.AllArgsConstructor;import lombok.Builder;
import lombok.Data;import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CardTokenMetadata {
    private String bin;
    private String lastFour;
    private String cardType;
    private String cardNetwork;
    private String cardholderName;
    private String expiryMonth;
    private String expiryYear;
    private TokenStatus status;
}
