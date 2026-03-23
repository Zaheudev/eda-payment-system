package com.zaheudev.ctm.entity;

import com.zaheudev.ctm.dto.TokenStatus;
import com.zaheudev.shared.avro.PaymentMethodEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @Entity @NoArgsConstructor @AllArgsConstructor
@ToString
public class CardTokenEntity {
    @Id
    private String tokenRef;
    private String tokenValue;
    private String encryptedPan;
    private String bin;
    private String lastFour;
    private String cardType;
    private String cardholderName;
    private String expiryMonth;
    private String expiryYear;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    @Enumerated(EnumType.STRING)
    private PaymentMethodEnum cardNetwork;
    @Enumerated(EnumType.STRING)
    private TokenStatus status;

    public void calculateExpiryAt(Integer monthsUntilExpire){
        this.expiresAt = createdAt.plusMonths(monthsUntilExpire);
    }
}
