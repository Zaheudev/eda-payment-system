package com.zaheudev.routing.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingCostTest {

    @Test
    void calculateTotalCostShouldApplyFormula() {
        RoutingCost cost = new RoutingCost();
        cost.setFixedFee(BigDecimal.valueOf(0.20));
        cost.setPercentageFee(BigDecimal.valueOf(0.015));
        cost.setAuthorizationRate(0.05);

        BigDecimal result = cost.calculateTotalCost(BigDecimal.valueOf(100));

        BigDecimal expected = BigDecimal.valueOf(0.015).multiply(BigDecimal.valueOf(100))
                .add(BigDecimal.valueOf(0.20)).add(BigDecimal.valueOf(0.05));
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    void calculateTotalCostWithZeroAmount() {
        RoutingCost cost = new RoutingCost();
        cost.setFixedFee(BigDecimal.valueOf(0.20));
        cost.setPercentageFee(BigDecimal.valueOf(0.015));
        cost.setAuthorizationRate(0.0);

        BigDecimal result = cost.calculateTotalCost(BigDecimal.ZERO);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(0.20));
    }
}
