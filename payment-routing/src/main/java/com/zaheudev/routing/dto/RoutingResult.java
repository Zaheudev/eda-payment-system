package com.zaheudev.routing.dto;

import com.zaheudev.shared.avro.PaymentMethodEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingResult {
    private PaymentMethodEnum selectedPaymentMethod;
    private BigDecimal calculatedFee;
    private BigDecimal transactionAmount;
    private String currency;
    private Boolean useToken;

    public static RoutingResult noValidOptions(BigDecimal amount, String currency) {
        return RoutingResult.builder()
                .selectedPaymentMethod(null)
                .calculatedFee(null)
                .transactionAmount(amount)
                .currency(currency)
                .useToken(false)
                .build();
    }

    public boolean hasValidOption() {
        return selectedPaymentMethod != null;
    }
}
