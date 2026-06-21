package com.zaheudev.vaadin.model;

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
    @Builder.Default
    private List<String> history = new ArrayList<>();
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
