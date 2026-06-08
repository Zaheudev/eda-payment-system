package com.zaheudev.gateway.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmountTest {

    @Test
    void ofShouldConvertToCentsAndUppercaseCurrency() {
        Amount amount = Amount.of(BigDecimal.valueOf(12.50), "usd");
        assertThat(amount.getAmount()).isEqualTo(1250L);
        assertThat(amount.getCurrency()).isEqualTo("USD");
    }

    @Test
    void ofShouldTrimCurrency() {
        Amount amount = Amount.of(BigDecimal.valueOf(10), "  eur ");
        assertThat(amount.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void ofShouldThrowOnNullAmount() {
        assertThatThrownBy(() -> Amount.of(null, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be non-null");
    }

    @Test
    void ofShouldThrowOnNegativeAmount() {
        assertThatThrownBy(() -> Amount.of(BigDecimal.valueOf(-1), "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void ofShouldThrowOnBlankCurrency(String currency) {
        assertThatThrownBy(() -> Amount.of(BigDecimal.ONE, currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency");
    }

    @Test
    void addShouldSumSameCurrency() {
        Amount a = Amount.of(BigDecimal.valueOf(10), "USD");
        Amount b = Amount.of(BigDecimal.valueOf(5), "USD");
        Amount result = a.add(b);
        assertThat(result.getAmount()).isEqualTo(1500L);
        assertThat(result.getCurrency()).isEqualTo("USD");
    }

    @Test
    void addShouldThrowOnDifferentCurrency() {
        Amount a = Amount.of(BigDecimal.valueOf(10), "USD");
        Amount b = Amount.of(BigDecimal.valueOf(5), "EUR");
        assertThatThrownBy(() -> a.add(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
    }

    @Test
    void subtractShouldWorkSameCurrency() {
        Amount a = Amount.of(BigDecimal.valueOf(10), "USD");
        Amount b = Amount.of(BigDecimal.valueOf(3), "USD");
        Amount result = a.subtract(b);
        assertThat(result.getAmount()).isEqualTo(700L);
        assertThat(result.getCurrency()).isEqualTo("USD");
    }

    @Test
    void subtractShouldThrowOnDifferentCurrency() {
        Amount a = Amount.of(BigDecimal.valueOf(10), "USD");
        Amount b = Amount.of(BigDecimal.valueOf(5), "EUR");
        assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
    }
}
