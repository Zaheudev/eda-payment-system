package com.zaheudev.demo.repository;

import com.zaheudev.demo.model.Payment;
import com.zaheudev.demo.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByPaymentId(String paymentId);
    List<Payment> findByStatus(PaymentStatus status);
}
