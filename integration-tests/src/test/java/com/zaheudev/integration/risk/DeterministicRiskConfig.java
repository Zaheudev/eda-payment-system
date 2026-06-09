package com.zaheudev.integration.risk;

import com.zaheudev.risk.model.RiskLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DeterministicRiskConfig {

    @Bean
    @Primary
    public DeterministicRiskService deterministicRiskService() {
        return new DeterministicRiskService();
    }
}
