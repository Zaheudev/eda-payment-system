package com.zaheudev.routing.entity;

import com.zaheudev.shared.avro.PaymentMethodEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor @AllArgsConstructor
public class RoutingCost {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Enumerated(EnumType.STRING)
    private PaymentMethodEnum paymentMethod;
    private BigDecimal fixedFee;
    private BigDecimal percentageFee;
    private Double authorizationRate;
    private Boolean isToken;

    /**
     * * Calculate the fee cost for a given amount based on the fixed fee, percentage fee, and authorization rate.
     * The formula will suffer changes later
     * @param amount
     * @return
     */
    public BigDecimal calculateTotalCost(Long amount) {;
        //i think this is the formula, but it can be changed if needed
        return (percentageFee.multiply(BigDecimal.valueOf(amount))).add(fixedFee).add(BigDecimal.valueOf(authorizationRate));
    }
}
