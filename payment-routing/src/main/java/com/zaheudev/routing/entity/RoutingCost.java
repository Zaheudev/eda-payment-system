package com.zaheudev.routing.entity;

import com.zaheudev.shared.avro.PaymentMethodEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor @AllArgsConstructor
public class RoutingCost {
    @Id
    private Long id;
    private PaymentMethodEnum paymentMethod;
    private BigDecimal fixedFee;
    private BigDecimal percentageFee;
    private Double authorizationRate;
    private Boolean isToken;
}
