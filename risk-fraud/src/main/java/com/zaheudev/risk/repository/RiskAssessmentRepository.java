package com.zaheudev.risk.repository;

import com.zaheudev.risk.entity.RiskAssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessmentEntity, String> {
    RiskAssessmentEntity findByTransactionId(String transactionId);
    RiskAssessmentEntity findByTransactionIdAndRiskLevel(String transactionId, String riskLevel);
}
