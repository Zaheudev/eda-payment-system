package com.zaheudev.bff.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaState {
    private String paymentId;
    private String currentState;
    private List<String> history;
    private Instant startedAt;
    private Instant lastUpdatedAt;

    public static SagaState createNew(String paymentId) {
        return SagaState.builder()
                .paymentId(paymentId)
                .currentState("CREATED")
                .history(new ArrayList<>())
                .startedAt(Instant.now())
                .lastUpdatedAt(Instant.now())
                .build();
    }

    public void advance(String newState) {
        if (!newState.equals(currentState)) {
            history.add(currentState);
            currentState = newState;
            lastUpdatedAt = Instant.now();
        }
    }
}
