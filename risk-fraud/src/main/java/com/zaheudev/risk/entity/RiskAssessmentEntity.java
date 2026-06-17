package com.zaheudev.risk.entity;

import com.zaheudev.risk.model.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity @Builder
@Data @NoArgsConstructor @AllArgsConstructor
public class RiskAssessmentEntity {
    @Id
    private String assessmentId;
    private String paymentId;
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;
    private String riskReason;
    private boolean approved;
    private LocalDate assessmentDate;

}
