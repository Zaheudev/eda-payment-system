package com.zaheudev.ctm.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DetokenizeResponse {
    private String cardNumber;
    private String expiryMonth;
    private String expiryYear;
}
