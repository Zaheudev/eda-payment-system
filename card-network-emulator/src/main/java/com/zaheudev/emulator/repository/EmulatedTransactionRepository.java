package com.zaheudev.emulator.repository;

import com.zaheudev.emulator.entity.EmulatedTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmulatedTransactionRepository extends JpaRepository<EmulatedTransactionEntity, String> {
    Optional<EmulatedTransactionEntity> findByPaymentId(String paymentId);
    boolean existsByPaymentId(String paymentId);
}
