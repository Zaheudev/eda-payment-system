package com.zaheudev.routing.repository;

import com.zaheudev.routing.entity.RoutingCostEntity;
import com.zaheudev.shared.avro.PaymentMethodEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RoutingCostRepository extends JpaRepository<RoutingCostEntity, Long> {
    List<RoutingCostEntity> findByPaymentMethodAndIsTokenIn(PaymentMethodEnum paymentMethod, Collection<Boolean> isTokens);
}