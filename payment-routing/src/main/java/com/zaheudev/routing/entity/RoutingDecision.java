package com.zaheudev.routing.entity;

import com.zaheudev.shared.avro.PaymentMethodEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data @NoArgsConstructor @AllArgsConstructor
public class RoutingDecision {
    @Id
    private Long id;
    private String paymentId;
    private PaymentMethodEnum selectedPaymentMethod;
    private BigDecimal estimatedCost;
    private Boolean useToken;
    private String availableNetworks;
    private LocalDateTime createdAt;

}
