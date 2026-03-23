package com.zaheudev.ctm.dto;

import com.zaheudev.shared.avro.PaymentMethodEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
public class TokenizeResponse {
    private String tokenRef;
    private String bin;
    private String lastFour;
    private String cardType;
    private PaymentMethodEnum cardNetwork;
    private String cardholderName;
    private String expiryMonth;
    private String expiryYear;
    private TokenStatus status;
}
