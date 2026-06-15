package com.zaheudev.risk.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskServiceTest {

    @Test
    void assessRiskShouldReturnNonNullRiskLevel() {
        RiskService riskService = new RiskService();
        var result = riskService.assessRisk();
        assertThat(result).isNotNull();
    }
}
