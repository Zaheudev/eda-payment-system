package com.zaheudev.routing.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingResultTest {

    @Test
    void hasValidOptionShouldReturnFalseWhenMethodNull() {
        RoutingResult result = RoutingResult.noValidOptions(BigDecimal.TEN, "USD");
        assertThat(result.hasValidOption()).isFalse();
        assertThat(result.getSelectedPaymentMethod()).isNull();
    }

    @Test
    void hasValidOptionShouldReturnTrueWhenMethodSet() {
        RoutingResult result = RoutingResult.builder()
                .selectedPaymentMethod(com.zaheudev.shared.avro.PaymentMethodEnum.VISA)
                .build();
        assertThat(result.hasValidOption()).isTrue();
    }

    @Test
    void noValidOptionsShouldPreserveAmountAndCurrency() {
        RoutingResult result = RoutingResult.noValidOptions(BigDecimal.valueOf(99.99), "USD");
        assertThat(result.getTransactionAmount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getUseToken()).isFalse();
    }
}
