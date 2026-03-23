package com.zaheudev.ctm.service;

import com.zaheudev.ctm.dto.DetokenizeResponse;
import com.zaheudev.ctm.dto.TokenStatus;
import com.zaheudev.ctm.dto.TokenizeResponse;
import com.zaheudev.ctm.entity.CardTokenEntity;
import com.zaheudev.ctm.repository.CardTokenRepository;
import com.zaheudev.shared.avro.CardDetails;
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

    private SecretKeySpec secretKey;
    @Value("${ctm.encryption.secret-key}")
    private String secretKeyString;
    private static final int EXPIRY_MONTHS = 12;

    @Autowired
    public CardTokenService(CardTokenRepository cardTokenRepository, Generator generator){
        this.cardTokenRepository = cardTokenRepository;
        this.generator = generator;
    }

    public TokenizeResponse tokenize(CardDetails cardDetails, String expiryMonth, String expiryYear) throws Exception {
        String tokenRef = "TKN-" + UUID.randomUUID().toString();
        String tokenValue = generator.generatePAN(12);
        String encryptedPan = generator.encryptPAN(cardDetails.getCardNumber().toString(),getSecretKey());
        log.info("Encrypted PAN: {} with tokenRef: {}", encryptedPan, tokenRef);

        CardTokenEntity tokenEntity = CardTokenEntity.builder()
                .tokenRef(tokenRef)
                .tokenValue(tokenValue)
                .encryptedPan(encryptedPan)
                .bin(cardDetails.getCardNumber().toString().substring(0, 6))
                .lastFour(cardDetails.getCardNumber().toString().substring(cardDetails.getCardNumber().toString().length() - 4))
                .cardNetwork(PaymentMethodEnum.VISA) // This should be determined by the card number pattern
                .cardType("CREDIT") // This should also be determined by the card number pattern
                .expiryMonth(expiryMonth)
                .expiryYear(expiryYear)
                .cardholderName(cardDetails.getCardHolderName().toString())
                .createdAt(LocalDateTime.now())
                .status(TokenStatus.ACTIVE)
                .build();
        tokenEntity.calculateExpiryAt(EXPIRY_MONTHS);
        cardTokenRepository.save(tokenEntity);
        log.info("Card token saved in db with tokenRef: {}", tokenRef);

        return TokenizeResponse.builder()
                .tokenRef(tokenRef)
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

    private SecretKey getSecretKey(){
        return new SecretKeySpec(secretKeyString.getBytes(StandardCharsets.UTF_8), "AES");
    }
}
