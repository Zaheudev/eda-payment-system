package com.zaheudev.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "payments")
@Entity @NoArgsConstructor
public class Payment {
    @Id @Getter
    @GeneratedValue(generator = "UUID")
    private String paymentId;
    @Getter
    private PaymentStatus status;
    @Getter
    private String tokenRef;
    @Getter
    private Long timestamp;
    @Getter
    private Long amount;
    @Getter
    private String currency;
}
