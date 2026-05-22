package com.zaheudev.emulator.repository;

import com.zaheudev.emulator.entity.EmulatedRefundTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmulatedRefundTransactionRepository extends JpaRepository<EmulatedRefundTransactionEntity, String> {
     List<EmulatedRefundTransactionEntity> findByRefundId(String refundId);
     List<EmulatedRefundTransactionEntity> findByPaymentId(String paymentId);
     boolean existsByRefundId(String refundId);
     boolean existsByPaymentId(String paymentId);
}
