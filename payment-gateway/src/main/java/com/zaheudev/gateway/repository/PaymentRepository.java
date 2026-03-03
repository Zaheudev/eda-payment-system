package com.zaheudev.gateway.repository;

import com.zaheudev.gateway.entity.PaymentEntity;
import com.zaheudev.gateway.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
    Optional<PaymentEntity> findByPaymentId(String paymentId);
    List<PaymentEntity> findByStatus(PaymentStatus status);
    List<PaymentEntity> getAllByOrderByCreatedAtDesc();
}
