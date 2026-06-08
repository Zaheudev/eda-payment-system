package com.zaheudev.risk.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RiskLevelTest {

    @ParameterizedTest
    @CsvSource({
            "0.0, LOW",
            "0.1, LOW",
            "0.2, LOW",
            "0.25, MEDIUM",
            "0.4, MEDIUM",
            "0.45, HIGH",
            "0.6, HIGH",
            "0.7, CRITICAL",
            "0.9, CRITICAL",
            "1.0, CRITICAL"
    })
    void fromScoreShouldReturnCorrectLevel(double score, RiskLevel expected) {
        assertThat(RiskLevel.fromScore(score)).isEqualTo(expected);
    }

    @Test
    void fromScoreShouldDefaultToCriticalForOutOfRange() {
        assertThat(RiskLevel.fromScore(-0.1)).isEqualTo(RiskLevel.CRITICAL);
        assertThat(RiskLevel.fromScore(1.5)).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void isWithinRangeShouldDetectBoundaries() {
        assertThat(RiskLevel.LOW.isWithinRange(0.0)).isTrue();
        assertThat(RiskLevel.LOW.isWithinRange(0.2)).isTrue();
        assertThat(RiskLevel.LOW.isWithinRange(0.21)).isFalse();

        assertThat(RiskLevel.MEDIUM.isWithinRange(0.2)).isTrue();
        assertThat(RiskLevel.MEDIUM.isWithinRange(0.4)).isTrue();

        assertThat(RiskLevel.HIGH.isWithinRange(0.4)).isTrue();
        assertThat(RiskLevel.HIGH.isWithinRange(0.6)).isTrue();

        assertThat(RiskLevel.CRITICAL.isWithinRange(0.6)).isTrue();
        assertThat(RiskLevel.CRITICAL.isWithinRange(1.0)).isTrue();
    }
}
