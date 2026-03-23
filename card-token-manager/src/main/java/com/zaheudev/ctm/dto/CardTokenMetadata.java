package com.zaheudev.ctm.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
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
