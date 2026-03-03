package com.zaheudev.routing.repository;

import com.zaheudev.routing.entity.RoutingDecision;

import java.util.Optional;

public interface RoutingDecisionRepository extends JpaRepository<RoutingDecision, Long> {
    Optional<RoutingDecision> findByPaymentId(String paymentId);
}
