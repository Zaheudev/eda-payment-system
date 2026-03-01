package com.zaheudev.demo.dto;

import lombok.*;
import org.apache.avro.specific.FixedSize;
import org.checkerframework.checker.index.qual.Positive;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    @NonNull @Getter
    private String merchantReference;
    @NonNull @Positive @Getter
    private BigDecimal amount;
    @NonNull @Getter
    private String currency;
    @Getter @NonNull
    private CardDetails cardDetails;
    @Getter @Setter
    private String tokenRef;

    public boolean isValid() {
        return (cardDetails != null && !hasTokenRef()) || (cardDetails == null && hasTokenRef());
    }

    public boolean hasTokenRef() {
        return tokenRef != null && tokenRef.isEmpty();
    }

    public boolean hasCardDetails() {
        return cardDetails != null;
    }
}
