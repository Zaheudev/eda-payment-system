package com.zaheudev.ctm.service;

import com.zaheudev.shared.dto.*;
import com.zaheudev.ctm.entity.CardTokenEntity;
import com.zaheudev.ctm.repository.CardTokenRepository;
import com.zaheudev.shared.avro.PaymentMethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service @Slf4j
public class CardTokenService {
    private CardTokenRepository cardTokenRepository;
    private Generator generator;

    @Value("${ctm.encryption.secret-key}")
    private String secretKeyString;
    private static final int EXPIRY_MONTHS = 12;

    @Autowired
    public CardTokenService(CardTokenRepository cardTokenRepository, Generator generator){
        this.cardTokenRepository = cardTokenRepository;
        this.generator = generator;
    }

    public TokenizeResponse tokenize(TokenizeRequest cardDetails) throws Exception { // throws Exception kept for encryptPAN
        validateCardInput(cardDetails);
        String tokenRef = "TKN-" + UUID.randomUUID().toString();
        String tokenValue = generator.generatePAN(16);
        String encryptedPan = generator.encryptPAN(cardDetails.getCardNumber().toString(),getSecretKey());
        String bin = cardDetails.getCardNumber().substring(0, 6);
        log.info("Encrypted PAN: {} with tokenRef: {}", encryptedPan, tokenRef);

        CardTokenEntity tokenEntity = CardTokenEntity.builder()
                .tokenRef(tokenRef)
                .tokenValue(tokenValue)
                .encryptedPan(encryptedPan)
                .bin(bin)
                .lastFour(cardDetails.getCardNumber().substring(cardDetails.getCardNumber().length() - 4))
                .cardNetwork(determinePrimaryNetwork(bin))
                .cardType(determineCardType(bin))
                .expiryMonth(cardDetails.getExpiryMonth())
                .expiryYear(cardDetails.getExpiryYear())
                .cardholderName(cardDetails.getCardHolderName())
                .createdAt(LocalDateTime.now())
                .status(TokenStatus.ACTIVE)
                .build();
        tokenEntity.calculateExpiryAt(EXPIRY_MONTHS);
        cardTokenRepository.save(tokenEntity);
        log.info("Card token saved in db with tokenRef: {}", tokenRef);

        return TokenizeResponse.builder()
                .tokenRef(tokenRef)
                .tokenValue(tokenValue)
                .bin(tokenEntity.getBin())
                .lastFour(tokenEntity.getLastFour())
                .cardNetwork(tokenEntity.getCardNetwork())
                .cardType(tokenEntity.getCardType())
                .expiryMonth(tokenEntity.getExpiryMonth())
                .expiryYear(tokenEntity.getExpiryYear())
                .cardholderName(tokenEntity.getCardholderName())
                .status(tokenEntity.getStatus())
                .build();

    }

    public void validateCardInput(TokenizeRequest cardDetails) {
        if (cardDetails.getCardNumber() == null) {
            throw new IllegalArgumentException("Card number is required");
        }
        int len = cardDetails.getCardNumber().length();
        if (len != 15 && len != 16) {
            throw new IllegalArgumentException("Card number must be 15 or 16 digits");
        }
        if (cardDetails.getCvv() == null || (cardDetails.getCvv().length() != 3 && cardDetails.getCvv().length() != 4)) {
            throw new IllegalArgumentException("CVV must be 3 or 4 digits");
        }
    }

    public DetokenizeResponse detokenize(String tokenRef) throws Exception{
        CardTokenEntity entity = cardTokenRepository.findByTokenRef(tokenRef)
                .orElseThrow(() -> new Exception("Token not found"));
        String decryptedPan = generator.decryptPAN(entity.getEncryptedPan(), getSecretKey());
        log.info("Decrypted PAN: {}", decryptedPan);
        return DetokenizeResponse.builder()
                .cardNumber(decryptedPan)
                .expiryYear(entity.getExpiryYear())
                .expiryMonth(entity.getExpiryMonth())
                .build();
    }

    public CardTokenMetadata getMetadata(String tokenRef){
        CardTokenEntity entity = cardTokenRepository.findByTokenRef(tokenRef)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        return CardTokenMetadata.builder()
                .bin(entity.getBin())
                .lastFour(entity.getLastFour())
                .cardType(entity.getCardType())
                .cardNetwork(entity.getCardNetwork().name())
                .cardholderName(entity.getCardholderName())
                .expiryMonth(entity.getExpiryMonth())
                .expiryYear(entity.getExpiryYear())
                .status(entity.getStatus())
                .build();
    }

    public TokenStatus updateStatus(String tokenRef, TokenStatus status){
        CardTokenEntity entity = cardTokenRepository.findByTokenRef(tokenRef)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        entity.setStatus(status);
        cardTokenRepository.save(entity);
        return status;
    }

    public TokenStatus getStatus(String tokenRef){
        CardTokenEntity entity = cardTokenRepository.findByTokenRef(tokenRef)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        return entity.getStatus();
    }

    private PaymentMethodEnum determinePrimaryNetwork(String bin) {
        if (bin.startsWith("4")) {
            return PaymentMethodEnum.VISA;
        } else if (bin.matches("^5[1-5].*")) {
            return PaymentMethodEnum.MASTERCARD;
        } else if (bin.startsWith("34") || bin.startsWith("37")) {
            return PaymentMethodEnum.AMEX;
        } else if (bin.startsWith("6")) {
            return PaymentMethodEnum.DISCOVER;
        }
        return PaymentMethodEnum.VISA;
    }

    private String determineCardType(String bin) {
        // in reality, this would come from a BIN database, but i am still figuring it out
        // for demo purposes: assume BIN xxx1 (if it ends with 1)
        return bin.endsWith("1") ? "DEBIT" : "CREDIT";
    }

    private SecretKey getSecretKey(){
        return new SecretKeySpec(secretKeyString.getBytes(StandardCharsets.UTF_8), "AES");
    }
}
