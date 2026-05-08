package com.zaheudev.risk.entity;

import com.zaheudev.risk.model.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity @Builder
@Data @NoArgsConstructor @AllArgsConstructor
public class RiskAssessmentEntity {
    @Id @Getter @Setter
    private String assessmentId;
    @Getter @Setter
    private String paymentId;
    @Getter @Setter @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;
    @Getter @Setter
    private String riskReason;
    @Getter @Setter
    private boolean approved;
    @Getter @Setter
    private LocalDate assessmentDate;

}
