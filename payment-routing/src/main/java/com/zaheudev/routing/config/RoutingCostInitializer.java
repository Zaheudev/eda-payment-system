package com.zaheudev.routing.config;

import com.zaheudev.routing.entity.RoutingCost;
import com.zaheudev.routing.repository.RoutingCostRepository;
import com.zaheudev.shared.avro.PaymentMethodEnum;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RoutingCostInitializer {

    private static final Logger log = LoggerFactory.getLogger(RoutingCostInitializer.class);

    private final RoutingCostRepository repository;

    public RoutingCostInitializer(RoutingCostRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void seedIfEmpty() {
        if (repository.count() > 0) {
            log.info("Routing cost table already seeded, skipping");
            return;
        }
        log.info("Seeding routing cost table with default values");
        repository.saveAll(List.of(
                rc(1L, PaymentMethodEnum.VISA,       0.10, 0.0150, 0.98, false),
                rc(2L, PaymentMethodEnum.VISA,       0.08, 0.0130, 0.98, true),
                rc(3L, PaymentMethodEnum.MASTERCARD, 0.10, 0.0155, 0.97, false),
                rc(4L, PaymentMethodEnum.MASTERCARD, 0.08, 0.0135, 0.97, true),
                rc(5L, PaymentMethodEnum.AMEX,       0.15, 0.0250, 0.95, false),
                rc(6L, PaymentMethodEnum.AMEX,       0.12, 0.0220, 0.95, true),
                rc(7L, PaymentMethodEnum.DISCOVER,   0.10, 0.0140, 0.96, false),
                rc(8L, PaymentMethodEnum.DISCOVER,   0.08, 0.0120, 0.96, true),
                rc(9L, PaymentMethodEnum.MAESTRO,    0.08, 0.0120, 0.96, false),
                rc(10L, PaymentMethodEnum.MAESTRO,   0.06, 0.0100, 0.96, true),
                rc(11L, PaymentMethodEnum.ACCEL,     0.05, 0.0080, 0.99, false),
                rc(12L, PaymentMethodEnum.STAR,      0.05, 0.0080, 0.99, false),
                rc(13L, PaymentMethodEnum.NYCE,      0.05, 0.0075, 0.99, false),
                rc(14L, PaymentMethodEnum.PULSE,     0.05, 0.0075, 0.99, false)
        ));
        log.info("Routing cost table seeded with {} rows", repository.count());
    }

    private static RoutingCost rc(Long id, PaymentMethodEnum method, double fixed, double pct, double authRate, boolean token) {
        RoutingCost c = new RoutingCost();
        c.setId(id);
        c.setPaymentMethod(method);
        c.setFixedFee(BigDecimal.valueOf(fixed));
        c.setPercentageFee(BigDecimal.valueOf(pct));
        c.setAuthorizationRate(authRate);
        c.setIsToken(token);
        return c;
    }
}
