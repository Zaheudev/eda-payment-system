package com.zaheudev.routing.repository;

import com.zaheudev.routing.entity.RoutingCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutingCostRepository extends JpaRepository<RoutingCost, Long> {
    List<RoutingCost> findByPaymentMethodAndIsToken(String paymentMethod, boolean isToken);
    List<RoutingCost> findByIsToken(boolean isToken);
}