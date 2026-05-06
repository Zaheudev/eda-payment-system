package com.zaheudev.risk.entity;

import com.zaheudev.risk.model.RiskLevel;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity @Builder
@Data @NoArgsConstructor @AllArgsConstructor
public class RiskAssessmentEntity {
    @Id
    private String assessmentId;
    private String paymentId;
    private RiskLevel riskLevel;
    private String riskReason;
    private boolean approved;
    private LocalDate assessmentDate;

}
