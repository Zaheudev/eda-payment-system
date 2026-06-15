package com.zaheudev.risk.model;

import lombok.AllArgsConstructor;
import lombok.Getter;import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public enum RiskLevel {
    LOW(0.0, 0.3, "Low Risk"),
    MEDIUM(0.3, 0.6, "Medium Risk"),
    HIGH(0.6, 0.8, "High Risk"),
    CRITICAL(0.8, 1.0, "Critical Risk");

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