package com.zaheudev.demo.dto;

import lombok.*;
import org.apache.avro.specific.FixedSize;
import org.checkerframework.checker.index.qual.Positive;

import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {
    @NonNull @Positive @Getter
    private BigDecimal amount;
    @NonNull @Getter
    private String currency;
    @Getter @NonNull
    private CardDetails cardDetails;

    @Data
    private class CardDetails {
        @Getter @Setter
        private String cardNumber;
        @Getter @Setter
        private String expiryMonth;
        @Getter @Setter
        private String expiryYear;
        @Getter @Setter
        private String cvv;
    }
}
