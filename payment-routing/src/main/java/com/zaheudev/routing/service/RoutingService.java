package com.zaheudev.routing.service;

import com.zaheudev.routing.dto.RoutingResult;
import com.zaheudev.routing.repository.RoutingCostRepository;
import com.zaheudev.shared.avro.PaymentMethodEnum;
import com.zaheudev.shared.avro.RiskAssessedEvent;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class RoutingService {
    @Autowired
    private RoutingCostRepository routingCostRepository;

    public RoutingResult calculateOptimalRouting(RiskAssessedEvent event){
        BigDecimal value = BigDecimal.valueOf(event.getAmount().getValue()).divide(BigDecimal.valueOf(100));
        boolean useToken = isTokenEligible(event);
        log.info("Calculating optimal routing for event: {}", event);
        Set<PaymentMethodEnum> availableNetworks = determineAvailableNetworks(event);
        log.info("Available networks: {}", availableNetworks);
        // in this map we store the total cost for every available network
        // we use TreeSet to insert them decreasing by the cost
        // For now a good approach is to use a TreeMap, but in future I will implement risk and other criteria to comporator which best is TreeSet
        // for long term TreeSet is the best and implementing.
        TreeSet<OptimalNetwork> options = new TreeSet<>(new Comparator<OptimalNetwork>() {
            @Override
            public int compare(OptimalNetwork o1, OptimalNetwork o2) {
                int cmp = o1.getCost().compareTo(o2.getCost());
                return cmp != 0 ? cmp : o1.getNetwork().name().compareTo(o2.getNetwork().name());
            }
        });

        Collection<Boolean> tokenOptions = useToken ? List.of(true, false) : List.of(false);
        for(PaymentMethodEnum network : availableNetworks){
            routingCostRepository.findByPaymentMethodAndIsTokenIn(network, tokenOptions).forEach(cost -> {
                BigDecimal calculatedFee = cost.calculateTotalCost(value);
                log.info("Calculated fee for {}: {}", network, calculatedFee);
                log.info("Value: " + value);
                log.info("variables: " + cost);
                options.add(new OptimalNetwork(network, calculatedFee, cost.getIsToken()));
            });
        }
        log.info("Routing options: {}", options);
        if(options.isEmpty()){
            return RoutingResult.noValidOptions(value, event.getAmount().getCurrency().toString());
        }
        log.info("Optimal option: {}", options.first());
        return RoutingResult.builder()
                .selectedPaymentMethod(options.first().getNetwork())
                .calculatedFee(options.first().cost)
                .transactionAmount(value)
                .currency(event.getAmount().getCurrency().toString())
                .useToken(options.first().useToken)
                .build();
    }

    /**
       * Determinates the available payment networks based on the card BIN and other criteria.
       * For simplicity, this example uses hardcoded rules from other functions, but in a real implementation,
       * this would likely involve looking up the BIN in a database to determine the card type and supported networks.
     * @return a set of available payment networks for the given payment request
     */
    public Set<PaymentMethodEnum> determineAvailableNetworks(RiskAssessedEvent event) {
        Set<PaymentMethodEnum> networks = new HashSet<>();
        PaymentMethodEnum primaryNetwork = event.getCardRecord().getPrimaryNetwork();
        networks.add(primaryNetwork);

        if (event.getCardRecord().getCardType().toString().equals("DEBIT")) {
            networks.addAll(getDebitNetworksForNetwork(primaryNetwork));
        }

        return networks;
    }

    private Set<PaymentMethodEnum> getDebitNetworksForNetwork(PaymentMethodEnum network) {
    // in reality this would come from a bin database,
    // but for demo purposes I hardcode some rules

    Set<PaymentMethodEnum> debitNetworks = new HashSet<>();
    if (network.equals(PaymentMethodEnum.VISA)) {
        debitNetworks.add(PaymentMethodEnum.ACCEL);
        debitNetworks.add(PaymentMethodEnum.STAR);
    }

    if (network.equals(PaymentMethodEnum.MASTERCARD)) {
        debitNetworks.add(PaymentMethodEnum.NYCE);
        debitNetworks.add(PaymentMethodEnum.PULSE);
    }

    if(network.equals(PaymentMethodEnum.DISCOVER)){
        debitNetworks.add(PaymentMethodEnum.PULSE);
    }

    return debitNetworks;
    }

    private boolean isTokenEligible(RiskAssessedEvent event){
        if(event.getCardRecord().getTokenValue() == null){
            log.warn("Token value is null for paymentId: {}", event.getPaymentId());
            return false;
        }
        return event.getCardRecord().getTokenValue() != null &&
                event.getCardRecord().getTokenStatus().toString().equals("ACTIVE");
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    private static class OptimalNetwork{
        private PaymentMethodEnum network;
        private BigDecimal cost;
        private boolean useToken;
    }
}
