package com.zaheudev.routing.entity;

import com.zaheudev.shared.avro.PaymentMethodEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Builder
@Data @NoArgsConstructor @AllArgsConstructor
public class RoutingDecision {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String paymentId;
    @Enumerated(EnumType.STRING)
    private PaymentMethodEnum selectedPaymentMethod;
    private BigDecimal calculatedFee;
    private Boolean useToken;
    private String availableNetworks;
    private LocalDateTime createdAt;

}
