package com.zaheudev.emulator.service;

import com.zaheudev.emulator.entity.EmulatedRefundTransactionEntity;
import com.zaheudev.shared.avro.CaptureCompletedEvent;
import com.zaheudev.emulator.client.CardMetadataClient;
import com.zaheudev.emulator.entity.EmulatedTransactionEntity;
import com.zaheudev.shared.avro.*;
import com.zaheudev.shared.dto.CardTokenMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service @Slf4j
public class EmulatedCardProcessor implements CardProcessor {
    @Autowired
    CardMetadataClient cardMetadataClient;

    private static final String ALLOWED_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int RRN_LENGTH = 12;
    private static final int TRANSACTION_ID_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${cne.latency.min-ms}")
    private int minLatency;
    @Value("${cne.latency.max-ms}")
    private int maxLatency;

    @Override
    public AuthorizationCompletedEvent authorize(String paymentId, CardRecord card,
                                                 PaymentMethodEnum selectedPaymentMethod,
                                                 BigDecimal amount, String currency) {
        CardTokenMetadata cardMetadata = cardMetadataClient.getMetadata(card.getTokenRef().toString());
        int expiryYear = Integer.parseInt(cardMetadata.getExpiryYear());
        int expiryMonth = Integer.parseInt(cardMetadata.getExpiryMonth());
        log.info("Precessing the authorization for paymentId: {}, cardHolderName: {}, selectedPaymentMethod: {}, amount: {}, currency: {}",
                paymentId, cardMetadata.getCardholderName(), selectedPaymentMethod, amount, currency);
        simulateNetworkLatency();
        if(expiryYear < LocalDate.now().getYear() ||
                (expiryYear == LocalDate.now().getYear() && expiryMonth < LocalDate.now().getMonthValue())){
            log.warn("Card expired for paymentId: {}", paymentId);
            return AuthorizationCompletedEvent.newBuilder()
                    .setSuccess(false)
                    .setPaymentId(paymentId)
                    .setProcessorTransactionId("PROC"+ UUID.randomUUID().toString().substring(TRANSACTION_ID_LENGTH))
                    .setRrn(generateRRN())
                    .setAuthCode(String.format("%06d", RANDOM.nextInt(1000000)))
                    .setSelectedPaymentMethod(selectedPaymentMethod)
                    .setErrorMessage("Card expired")
                    .setTimestamp(System.currentTimeMillis())
                    .build();
        }
        if(RANDOM.nextInt(100) < 10){ // 10% chance of failure
            log.warn("Authorization failed for paymentId: {}", paymentId);
            return AuthorizationCompletedEvent.newBuilder()
                    .setSuccess(false)
                    .setPaymentId(paymentId)
                    .setProcessorTransactionId("PROC"+ UUID.randomUUID().toString().substring(TRANSACTION_ID_LENGTH))
                    .setRrn(generateRRN())
                    .setAuthCode(String.format("%06d", RANDOM.nextInt(1000000)))
                    .setSelectedPaymentMethod(selectedPaymentMethod)
                    .setErrorMessage("Authorization declined by issuer. Contact issuer for more details.")
                    .setTimestamp(System.currentTimeMillis())
                    .build();
        }
        AuthorizationCompletedEvent result = AuthorizationCompletedEvent.newBuilder()
                .setSuccess(true)
                .setPaymentId(paymentId)
                .setProcessorTransactionId("PROC"+ UUID.randomUUID().toString().substring(TRANSACTION_ID_LENGTH))
                .setRrn(generateRRN())
                .setAuthCode(String.format("%06d", RANDOM.nextInt(1000000)))
                .setSelectedPaymentMethod(selectedPaymentMethod)
                .setTimestamp(System.currentTimeMillis())
                .build();
        log.info("Authorization completed for paymentId: {}", paymentId);
        return result;
    }

    @Override
    public CaptureCompletedEvent capture(EmulatedTransactionEntity entity) {
        simulateNetworkLatency();
        return CaptureCompletedEvent.newBuilder()
                .setSuccess(RANDOM.nextInt(100) > 5)
                .setPaymentId(entity.getPaymentId())
                .setProcessorTransactionId(entity.getProcessorTransactionId())
                .setCaptureId(UUID.randomUUID().toString().substring(TRANSACTION_ID_LENGTH))
                .setRrn(entity.getRrn())
                .setAuthCode(entity.getAuthCode())
                .setAmount(Amount.newBuilder()
                        .setValue(entity.getAuthorizedAmount().longValue())
                        .setCurrency(entity.getCurrency())
                        .build())
                .setNetworkFee(entity.getNetworkFee().toString())
                .setTimestamp(System.currentTimeMillis())
                .build();
    }

    @Override
    public RefundCompletedEvent refund(String paymentId, String originalProcessorTransactionId, BigDecimal refundAmountTotal, String currency) {
        simulateNetworkLatency();
        if(RANDOM.nextInt(100) < 5){
            log.error("Refund failed for paymentId: {}", paymentId);
            log.error("Refund refused by issuer, payment id: {}", paymentId);
            log.error("continuing processing....");
            return RefundCompletedEvent.newBuilder()
                    .setSuccess(false)
                    .setPaymentId(paymentId)
                    .setRefundId(UUID.randomUUID().toString().substring(TRANSACTION_ID_LENGTH))
                    .setErrorMessage("Refund failed due to issuer error. Please try again later.")
                    .setTimeStamp(System.currentTimeMillis())
                    .build();
        }

        return RefundCompletedEvent.newBuilder()
                .setSuccess(true)
                .setPaymentId(paymentId)
                .setOriginalProcessorTransactionId(originalProcessorTransactionId)
                .setRefundId("REFUND-"+UUID.randomUUID().toString().substring(TRANSACTION_ID_LENGTH))
                .setCurrency(currency)
                .setRefundedAmount(Amount.newBuilder()
                        .setValue(refundAmountTotal.multiply(BigDecimal.valueOf(100)).longValue())
                        .setCurrency(currency)
                        .build())
                .setCreatedAt(LocalDate.now().toString())
                .setUpdatedAt(LocalDate.now().toString())
                .setTimeStamp(System.currentTimeMillis())
                .setRefundRrn(generateRRN())
                .setRefundAuthCode(String.format("%06d", RANDOM.nextInt(1000000)))
                .build();
    }

    @Override
    public AuthorizationCompletedEvent voidTransaction(String paymentId) {
        return null;
    }


    public void simulateNetworkLatency(){
        try {
            Thread.sleep(RANDOM.nextInt(maxLatency - minLatency) + minLatency);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String generateRRN(){
        StringBuilder sb = new StringBuilder(RRN_LENGTH);
        for(int i=0; i<RRN_LENGTH; i++){
            sb.append(ALLOWED_CHARS.charAt(RANDOM.nextInt(ALLOWED_CHARS.length())));
        }
        return sb.toString();
    }

}
