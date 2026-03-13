package com.zaheudev.routing.repository;

import com.zaheudev.routing.entity.RoutingDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoutingDecisionRepository extends JpaRepository<RoutingDecision, Long> {
    Optional<RoutingDecision> findByPaymentId(String paymentId);
}
