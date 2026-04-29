package com.zaheudev.routing.service;

import com.zaheudev.routing.dto.RoutingResult;
import com.zaheudev.routing.repository.RoutingCostRepository;
import com.zaheudev.shared.avro.PaymentMethodEnum;
import com.zaheudev.shared.avro.PaymentRequestedEvent;
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

    public RoutingResult calculateOptimalRouting(PaymentRequestedEvent event){
        Long value = event.getAmount().getValue();
        log.info("Calculating optimal routing for event: {}", event);
        Set<PaymentMethodEnum> availableNetworks = determineAvailableNetworks(event);
        log.info("Available networks: {}", availableNetworks);
        // in this map we store the total cost for every available network
        // we use TreeSet to insert them decreasing by the cost
        // For now a good approach is to use a TreeMap, but in future I will implement risk and other criteria to comporator which best is TreeSet
        // for long term TreeSet is the best and implementing.
        TreeSet<OptimalNetwork<PaymentMethodEnum, BigDecimal>> options = new TreeSet<>(new Comparator<OptimalNetwork<PaymentMethodEnum, BigDecimal>>() {
            @Override
            public int compare(OptimalNetwork<PaymentMethodEnum, BigDecimal> o1, OptimalNetwork<PaymentMethodEnum, BigDecimal> o2) {
                int cmp = o1.getCost().compareTo(o2.getCost());
                return cmp != 0 ? cmp : o1.getNetwork().name().compareTo(o2.getNetwork().name());
            }
        });
        for(PaymentMethodEnum network : availableNetworks){
            // for now by default we parse token false because we dont have build yet
            // the card token manager module. In the future, when we have the
            // tokenization module, we will determine if we can use a token or not
            // based on the event data and the tokenization status of the card.
            routingCostRepository.findByPaymentMethodAndIsToken(network, false).forEach(cost -> {
                BigDecimal calculatedFee = cost.calculateTotalCost(value);
                log.warn("Calculated fee for {}: {}", network, calculatedFee);
                log.warn("Value: " + value);
                log.warn("variables: " + cost);
                options.add(new OptimalNetwork<>(network, calculatedFee));
            });
        }
        log.info("Routing options: {}", options);
        if(options.isEmpty()){
            return RoutingResult.noValidOptions(BigDecimal.valueOf(value), event.getAmount().getCurrency().toString());
        }
        return RoutingResult.builder()
                .selectedPaymentMethod(options.first().getNetwork())
                .calculatedFee(options.first().cost)
                .transactionAmount(BigDecimal.valueOf(value))
                .currency(event.getAmount().getCurrency().toString())
                .useToken(false)
                .build();
    }

    /**
       * Determinates the available payment networks based on the card BIN and other criteria.
       * For simplicity, this example uses hardcoded rules from other functions, but in a real implementation,
       * this would likely involve looking up the BIN in a database to determine the card type and supported networks.
     * @return a set of available payment networks for the given payment request
     */
    public Set<PaymentMethodEnum> determineAvailableNetworks(PaymentRequestedEvent event) {
        Set<PaymentMethodEnum> networks = new HashSet<>();

        String bin = event.getCardRecord().getBin().toString();
        PaymentMethodEnum primaryNetwork = determinePrimaryNetwork(bin);
        networks.add(primaryNetwork);

        if (isDebitCard(bin)) {
            networks.addAll(getDebitNetworksForBin(bin));
        }

        return networks;
    }

    private PaymentMethodEnum determinePrimaryNetwork(String bin) {
        if (bin.startsWith("4")) {
            return PaymentMethodEnum.VISA;
        } else if (bin.matches("^5[1-5].*")) {
            return PaymentMethodEnum.MASTERCARD;
        } else if (bin.startsWith("34") || bin.startsWith("37")) {
            return PaymentMethodEnum.AMEX;
        } else if (bin.startsWith("6")) {
            return PaymentMethodEnum.DISCOVER;
        }
        return PaymentMethodEnum.VISA;
    }

    private boolean isDebitCard(String bin) {
        // in reality, this would come from a BIN database, but i am still figuring it out
        // for demo purposes: assume BINs 453xxx and 520xxx are debit
        return bin.startsWith("453") || bin.startsWith("520");
    }

    private Set<PaymentMethodEnum> getDebitNetworksForBin(String bin) {
        // in reality this would come from a bin database,
        // but for demo purposes I hardcode some rules

        Set<PaymentMethodEnum> debitNetworks = new HashSet<>();

        if (bin.startsWith("453")) {
            debitNetworks.add(PaymentMethodEnum.ACCEL);
            debitNetworks.add(PaymentMethodEnum.STAR);
        }

        if (bin.startsWith("520")) {
            debitNetworks.add(PaymentMethodEnum.NYCE);
            debitNetworks.add(PaymentMethodEnum.PULSE);
        }

        return debitNetworks;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    private class OptimalNetwork<K,V>{
        @Getter
        private K network;
        @Getter
        private V cost;
    }
}
