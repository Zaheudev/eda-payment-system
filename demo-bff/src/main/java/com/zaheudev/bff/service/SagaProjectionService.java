package com.zaheudev.bff.service;

import com.zaheudev.bff.model.SagaState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SagaProjectionService {

    private final Map<String, SagaState> sagas = new ConcurrentHashMap<>();

    public void onEvent(String paymentId, String eventType) {
        if (paymentId == null || paymentId.isBlank()) return;

        SagaState state = sagas.computeIfAbsent(paymentId, SagaState::createNew);

        switch (eventType) {
            case "PaymentRequestedEvent" -> state.advance("CREATED");
            case "RiskAssessedEvent" -> {
                if (state.getCurrentState().equals("CREATED") || state.getCurrentState().equals("RISK_ASSESSING")) {
                    state.advance("RISK_ASSESSED");
                }
            }
            case "PaymentRejectedEvent" -> state.advance("REJECTED");
            case "RoutedCompletedEvent" -> state.advance("ROUTED");
            case "AuthorizationCompletedEvent" -> state.advance("AUTHORIZED");
            case "CaptureCompletedEvent" -> state.advance("CAPTURED");
            case "VoidCompletedEvent" -> state.advance("VOIDED");
            case "RefundCompletedEvent" -> state.advance("REFUNDED");
            default -> {}
        }
    }

    public SagaState getSaga(String paymentId) {
        return sagas.get(paymentId);
    }

    public Collection<SagaState> getAllSagas() {
        return sagas.values();
    }

    public void clear() {
        sagas.clear();
    }
}
