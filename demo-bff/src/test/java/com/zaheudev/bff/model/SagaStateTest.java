package com.zaheudev.bff.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SagaStateTest {

    @Test
    void createNewShouldSetInitialState() {
        SagaState state = SagaState.createNew("PMT001");

        assertThat(state.getPaymentId()).isEqualTo("PMT001");
        assertThat(state.getCurrentState()).isEqualTo("CREATED");
        assertThat(state.getHistory()).isEmpty();
        assertThat(state.getStartedAt()).isNotNull();
        assertThat(state.getLastUpdatedAt()).isNotNull();
    }

    @Test
    void advanceShouldRecordHistoryAndUpdateState() {
        SagaState state = SagaState.createNew("PMT001");
        state.advance("RISK_ASSESSED");

        assertThat(state.getCurrentState()).isEqualTo("RISK_ASSESSED");
        assertThat(state.getHistory()).containsExactly("CREATED");
    }

    @Test
    void advanceShouldNotRecordDuplicateTransitions() {
        SagaState state = SagaState.createNew("PMT001");
        state.advance("CREATED");

        assertThat(state.getCurrentState()).isEqualTo("CREATED");
        assertThat(state.getHistory()).isEmpty();
    }

    @Test
    void advanceShouldUpdateTimestamp() {
        SagaState state = SagaState.createNew("PMT001");
        var before = state.getLastUpdatedAt();
        state.advance("AUTHORIZED");

        assertThat(state.getLastUpdatedAt()).isAfter(before);
    }

    @Test
    void fullSagaShouldRecordAllTransitions() {
        SagaState state = SagaState.createNew("PMT001");
        state.advance("RISK_ASSESSED");
        state.advance("ROUTED");
        state.advance("AUTHORIZED");
        state.advance("CAPTURED");

        assertThat(state.getCurrentState()).isEqualTo("CAPTURED");
        assertThat(state.getHistory()).containsExactly("CREATED", "RISK_ASSESSED", "ROUTED", "AUTHORIZED");
    }
}
