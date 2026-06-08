package com.zaheudev.routing.service;

import com.zaheudev.routing.entity.RoutingCost;
import com.zaheudev.routing.repository.RoutingCostRepository;
import com.zaheudev.shared.avro.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingServiceTest {

    @Mock
    private RoutingCostRepository routingCostRepository;

    @InjectMocks
    private RoutingService routingService;

    private RiskAssessedEvent buildEvent(String cardType) {
        return RiskAssessedEvent.newBuilder()
                .setAssessmentId("ASM-001")
                .setPaymentId("PMT001")
                .setRiskLevel(com.zaheudev.shared.avro.RiskLevel.LOW)
                .setReason("ok")
                .setApproved(true)
                .setAmount(Amount.newBuilder().setValue(10000L).setCurrency("USD").build())
                .setCardRecord(CardRecord.newBuilder()
                        .setTokenRef("TKN-abc")
                        .setTokenValue("tk-123")
                        .setBin("411111")
                        .setLastFour("1111")
                        .setTokenStatus("ACTIVE")
                        .setPrimaryNetwork(PaymentMethodEnum.VISA)
                        .setCardType(cardType)
                        .build())
                .setTimestamp(System.currentTimeMillis())
                .build();
    }

    @Test
    void calculateOptimalRoutingShouldPickLowestCost() {
        RiskAssessedEvent event = buildEvent("CREDIT");
        RoutingCost cheap = buildCost(PaymentMethodEnum.VISA, BigDecimal.valueOf(0.10), true);
        RoutingCost expensive = buildCost(PaymentMethodEnum.VISA, BigDecimal.valueOf(0.30), false);
        when(routingCostRepository.findByPaymentMethodAndIsTokenIn(any(), anyCollection()))
                .thenReturn(List.of(cheap, expensive));

        var result = routingService.calculateOptimalRouting(event);

        assertThat(result.hasValidOption()).isTrue();
        assertThat(result.getSelectedPaymentMethod()).isEqualTo(PaymentMethodEnum.VISA);
        assertThat(result.getCalculatedFee()).isEqualByComparingTo(BigDecimal.valueOf(0.10));
    }

    @Test
    void calculateOptimalRoutingShouldReturnNoValidOptionsWhenEmpty() {
        RiskAssessedEvent event = buildEvent("CREDIT");
        when(routingCostRepository.findByPaymentMethodAndIsTokenIn(any(), anyCollection()))
                .thenReturn(List.of());

        var result = routingService.calculateOptimalRouting(event);

        assertThat(result.hasValidOption()).isFalse();
    }

    @Test
    void determineAvailableNetworksShouldAddDebitNetworksForVisa() {
        RiskAssessedEvent event = buildEvent("DEBIT");
        Set<PaymentMethodEnum> networks = routingService.determineAvailableNetworks(event);
        assertThat(networks).contains(PaymentMethodEnum.VISA, PaymentMethodEnum.ACCEL, PaymentMethodEnum.STAR);
    }

    @Test
    void determineAvailableNetworksShouldAddDebitNetworksForMastercard() {
        RiskAssessedEvent event = RiskAssessedEvent.newBuilder()
                .setAssessmentId("ASM-001")
                .setPaymentId("PMT001")
                .setRiskLevel(com.zaheudev.shared.avro.RiskLevel.LOW)
                .setReason("ok")
                .setApproved(true)
                .setTimestamp(System.currentTimeMillis())
                .setCardRecord(CardRecord.newBuilder()
                        .setTokenRef("TKN-abc")
                        .setBin("511111")
                        .setLastFour("1111")
                        .setPrimaryNetwork(PaymentMethodEnum.MASTERCARD)
                        .setCardType("DEBIT")
                        .build())
                .build();
        Set<PaymentMethodEnum> networks = routingService.determineAvailableNetworks(event);
        assertThat(networks).contains(PaymentMethodEnum.MASTERCARD, PaymentMethodEnum.NYCE, PaymentMethodEnum.PULSE);
    }

    @Test
    void determineAvailableNetworksShouldAddDebitNetworksForDiscover() {
        RiskAssessedEvent event = RiskAssessedEvent.newBuilder()
                .setAssessmentId("ASM-001")
                .setPaymentId("PMT001")
                .setRiskLevel(com.zaheudev.shared.avro.RiskLevel.LOW)
                .setReason("ok")
                .setApproved(true)
                .setTimestamp(System.currentTimeMillis())
                .setCardRecord(CardRecord.newBuilder()
                        .setTokenRef("TKN-abc")
                        .setBin("601111")
                        .setLastFour("1117")
                        .setPrimaryNetwork(PaymentMethodEnum.DISCOVER)
                        .setCardType("DEBIT")
                        .build())
                .build();
        Set<PaymentMethodEnum> networks = routingService.determineAvailableNetworks(event);
        assertThat(networks).contains(PaymentMethodEnum.DISCOVER, PaymentMethodEnum.PULSE);
    }

    @Test
    void determineAvailableNetworksShouldReturnOnlyPrimaryForCredit() {
        RiskAssessedEvent event = buildEvent("CREDIT");
        Set<PaymentMethodEnum> networks = routingService.determineAvailableNetworks(event);
        assertThat(networks).containsExactly(PaymentMethodEnum.VISA);
    }

    private RoutingCost buildCost(PaymentMethodEnum method, BigDecimal fixedFee, boolean isToken) {
        RoutingCost cost = new RoutingCost();
        cost.setPaymentMethod(method);
        cost.setFixedFee(fixedFee);
        cost.setPercentageFee(BigDecimal.ZERO);
        cost.setAuthorizationRate(0.0);
        cost.setIsToken(isToken);
        return cost;
    }
}
