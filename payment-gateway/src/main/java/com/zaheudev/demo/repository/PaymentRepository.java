package com.zaheudev.demo.repository;

import com.zaheudev.demo.entity.PaymentEntity;
import com.zaheudev.demo.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
    Optional<PaymentEntity> findByPaymentId(String paymentId);
    List<PaymentEntity> findByStatus(PaymentStatus status);
}
