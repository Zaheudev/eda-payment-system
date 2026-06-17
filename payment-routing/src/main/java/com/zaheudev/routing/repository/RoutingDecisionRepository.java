package com.zaheudev.routing.repository;

import com.zaheudev.routing.entity.RoutingDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoutingDecisionRepository extends JpaRepository<RoutingDecisionEntity, Long> {
    Optional<RoutingDecisionEntity> findByPaymentId(String paymentId);
}
