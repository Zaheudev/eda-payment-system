package com.zaheudev.risk.model;

import lombok.AllArgsConstructor;
import lombok.Getter;import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public enum RiskLevel {
    LOW(0.0, 0.2, "Low Risk"),
    MEDIUM(0.2, 0.4, "Medium Risk"),
    HIGH(0.4, 0.6, "High Risk"),
    CRITICAL(0.6, 1.0, "Critical Risk");

    private double minScore;
    private double maxScore;
    @Getter
    private String description;

    public boolean isWithinRange(double score){
        return score >= minScore && score <= maxScore;
    }

    public static RiskLevel fromScore(double score){
        for (RiskLevel level : values()){
            if (level.isWithinRange(score)){
                return level;
            }
        }
        return CRITICAL; // Default to CRITICAL if score is out of expected range
    }

}