package com.zaheudev.integration.risk;

import com.zaheudev.risk.model.RiskLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(name = "test.deterministic.risk", havingValue = "true", matchIfMissing = false)
public class DeterministicRiskService extends com.zaheudev.risk.service.RiskService {

    private static volatile boolean rejectNext = false;

    public static void setRejectNext(boolean reject) {
        rejectNext = reject;
    }

    public static void resetToApprove() {
        rejectNext = false;
    }

    @Override
    public RiskLevel assessRisk(String transactionId) {
        if (rejectNext) {
            return RiskLevel.CRITICAL;
        }
        return RiskLevel.LOW;
    }
}
