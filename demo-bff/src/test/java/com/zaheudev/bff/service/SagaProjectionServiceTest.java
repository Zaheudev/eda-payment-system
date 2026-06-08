package com.zaheudev.bff.service;

import com.zaheudev.bff.model.SagaState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SagaProjectionServiceTest {

    private SagaProjectionService service;

    @BeforeEach
    void setUp() {
        service = new SagaProjectionService();
    }

    @Test
    void onEventShouldTransitionFromCreatedToRiskAssessed() {
        service.onEvent("PMT001", "PaymentRequestedEvent");
        service.onEvent("PMT001", "RiskAssessedEvent");

        SagaState saga = service.getSaga("PMT001");
        assertThat(saga.getCurrentState()).isEqualTo("RISK_ASSESSED");
    }

    @Test
    void onEventShouldHandleRejection() {
        service.onEvent("PMT001", "PaymentRequestedEvent");
        service.onEvent("PMT001", "PaymentRejectedEvent");

        SagaState saga = service.getSaga("PMT001");
        assertThat(saga.getCurrentState()).isEqualTo("REJECTED");
    }

    @Test
    void onEventShouldHandleFullLifecycle() {
        service.onEvent("PMT001", "PaymentRequestedEvent");
        service.onEvent("PMT001", "RiskAssessedEvent");
        service.onEvent("PMT001", "RoutedCompletedEvent");
        service.onEvent("PMT001", "AuthorizationCompletedEvent");
        service.onEvent("PMT001", "CaptureCompletedEvent");

        SagaState saga = service.getSaga("PMT001");
        assertThat(saga.getCurrentState()).isEqualTo("CAPTURED");
    }

    @Test
    void onEventShouldIgnoreNullPaymentId() {
        service.onEvent(null, "PaymentRequestedEvent");
        assertThat(service.getAllSagas()).isEmpty();
    }

    @Test
    void onEventShouldIgnoreBlankPaymentId() {
        service.onEvent("  ", "PaymentRequestedEvent");
        assertThat(service.getAllSagas()).isEmpty();
    }

    @Test
    void onEventShouldIgnoreUnknownEventType() {
        service.onEvent("PMT001", "UnknownEvent");
        SagaState saga = service.getSaga("PMT001");
        assertThat(saga.getCurrentState()).isEqualTo("CREATED");
    }

    @Test
    void getSagaShouldReturnNullForUnknownPayment() {
        assertThat(service.getSaga("NONEXIST")).isNull();
    }

    @Test
    void getAllSagasShouldReturnAllTrackedSagas() {
        service.onEvent("PMT001", "PaymentRequestedEvent");
        service.onEvent("PMT002", "PaymentRequestedEvent");

        assertThat(service.getAllSagas()).hasSize(2);
    }

    @Test
    void clearShouldRemoveAllSagas() {
        service.onEvent("PMT001", "PaymentRequestedEvent");
        service.clear();

        assertThat(service.getAllSagas()).isEmpty();
        assertThat(service.getSaga("PMT001")).isNull();
    }

    @Test
    void voidCompletedShouldAdvanceToVoided() {
        service.onEvent("PMT001", "PaymentRequestedEvent");
        service.onEvent("PMT001", "VoidCompletedEvent");

        assertThat(service.getSaga("PMT001").getCurrentState()).isEqualTo("VOIDED");
    }

    @Test
    void refundCompletedShouldAdvanceToRefunded() {
        service.onEvent("PMT001", "PaymentRequestedEvent");
        service.onEvent("PMT001", "RefundCompletedEvent");

        assertThat(service.getSaga("PMT001").getCurrentState()).isEqualTo("REFUNDED");
    }
}
