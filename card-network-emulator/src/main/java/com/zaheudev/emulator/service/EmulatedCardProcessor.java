package com.zaheudev.emulator.service;

import com.zaheudev.emulator.model.TransactionStatus;
import com.zaheudev.emulator.repository.EmulatedTransactionRepository;
import com.zaheudev.shared.avro.CaptureCompletedEvent;
import com.zaheudev.emulator.client.CardMetadataClient;
import com.zaheudev.emulator.entity.EmulatedTransactionEntity;
import com.zaheudev.shared.avro.*;
import com.zaheudev.shared.dto.CardTokenMetadata;
import com.zaheudev.shared.dto.TokenStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.UUID;

@Service @Slf4j
public class EmulatedCardProcessor{
    @Autowired
    CardMetadataClient cardMetadataClient;

    @Autowired
    EmulatedTransactionRepository transactionRepository;

    private static final String ALLOWED_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int RRN_LENGTH = 12;
    private static final int TRANSACTION_ID_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${cne.latency.min-ms}")
    private int minLatency;
    @Value("${cne.latency.max-ms}")
    private int maxLatency;
    @Value("${cne.authorization-failure-percent:-1}")
    private int authorizationFailurePercent;

    public AuthorizationCompletedEvent authorize(RoutedCompletedEvent event) {
        simulateNetworkLatency();
        boolean validCard;
        if(event.getUseToken()){
            validCard = isTokenActive(event.getCardRecord()) && isCardValid(event.getCardRecord());
        }else {
            validCard = isCardValid(event.getCardRecord());
        }
        log.info("Precessing the authorization for paymentId: {}, selectedPaymentMethod: {}, amount: {}, useToken: {}",
                event.getPaymentId(), event.getSelectedPaymentMethod(), event.getAmount().toString(), event.getUseToken());
        if(!validCard){
            String errorMessage = event.getUseToken() ? "Token expired" : "Card expired";
            log.warn(errorMessage+" for paymentId: {}", event.getPaymentId());
            return AuthorizationCompletedEvent.newBuilder()
                    .setSuccess(false)
                    .setPaymentId(event.getPaymentId())
                    .setProcessorTransactionId(null)
                    .setRrn(null)
                    .setAuthCode(null)
                    .setSelectedPaymentMethod(event.getSelectedPaymentMethod())
                    .setErrorMessage(errorMessage)
                    .setTimestamp(System.currentTimeMillis())
                    .build();
        }
        int failureRate = authorizationFailurePercent >= 0
                ? authorizationFailurePercent
                : (event.getUseToken() ? 3 : 10);
        if(RANDOM.nextInt(100) < failureRate){
            log.warn("Authorization failed for paymentId: {}", event.getPaymentId());
            return AuthorizationCompletedEvent.newBuilder()
                    .setSuccess(false)
                    .setPaymentId(event.getPaymentId())
                    .setProcessorTransactionId("PROC"+ UUID.randomUUID().toString().substring(TRANSACTION_ID_LENGTH))
                    .setRrn(generateRRN())
                    .setAuthCode("FAILED-"+String.format("%06d", RANDOM.nextInt(1000000)))
                    .setSelectedPaymentMethod(event.getSelectedPaymentMethod())
                    .setErrorMessage("Authorization declined by issuer. Contact issuer for more details.")
                    .setTimestamp(System.currentTimeMillis())
                    .build();
        }
        AuthorizationCompletedEvent result = AuthorizationCompletedEvent.newBuilder()
                .setSuccess(true)
                .setPaymentId(event.getPaymentId())
                .setProcessorTransactionId("PROC"+ UUID.randomUUID().toString().substring(TRANSACTION_ID_LENGTH))
                .setRrn(generateRRN())
                .setAuthCode(String.format("%06d", RANDOM.nextInt(1000000)))
                .setSelectedPaymentMethod(event.getSelectedPaymentMethod())
                .setTimestamp(System.currentTimeMillis())
                .build();
        log.info("Authorization completed for paymentId: {}", event.getPaymentId());
        return result;
    }

    public boolean isCardValid(CardRecord card){
        try{
        CardTokenMetadata cardMetadata = cardMetadataClient.getMetadata(card.getTokenRef().toString());
        int expiryYear = Integer.parseInt(cardMetadata.getExpiryYear());
        int expiryMonth = Integer.parseInt(cardMetadata.getExpiryMonth());
        log.info("Check for card expiry month and year, cardHolderName: {}, expiryMonth: {}, expiryYear: {}",
                cardMetadata.getCardholderName(), expiryMonth, expiryYear);
        return expiryYear > LocalDate.now().getYear() ||
                (expiryYear == LocalDate.now().getYear() && expiryMonth >= LocalDate.now().getMonthValue());
        }catch (Exception e){
            log.error("Error checking card expiry: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenActive(CardRecord cardRecord){
        try{
            TokenStatus tokenStatus = cardMetadataClient.getTokenStatus(cardRecord.getTokenRef().toString());
            log.info("Check for token status, tokenRef: {}, tokenStatus: {}", cardRecord.getTokenRef(), tokenStatus);
            return tokenStatus == TokenStatus.ACTIVE;
        }catch(Exception e){
            log.error("Error checking token status: {}", e.getMessage());
            return false;
        }
    }

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

    public VoidCompletedEvent voidTransaction(String paymentId) {
        simulateNetworkLatency();
        EmulatedTransactionEntity entity = transactionRepository.findByPaymentId(paymentId).orElseThrow(()->
                new RuntimeException("Payment not found with id: " + paymentId));
        if(entity.getTransactionStatus() != TransactionStatus.AUTHORIZED){
            log.error("Payment with id {} is not authorized. Current status: {}. Skipping processing.", paymentId, entity.getTransactionStatus());
            return VoidCompletedEvent.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage("Only authorized transactions can be voided")
                    .setPaymentId(paymentId)
                    .setProcessortransactionId(entity.getProcessorTransactionId())
                    .setTimeStamp(System.currentTimeMillis())
                    .setCreatedAt(LocalDate.now().toString())
                    .setUpdatedAt(LocalDate.now().toString())
                    .build();
        }
        boolean accepted = RANDOM.nextInt(100) > 5; // 5% chance of failure
        entity.setTransactionStatus(TransactionStatus.VOID);
        transactionRepository.save(entity);
        return VoidCompletedEvent.newBuilder()
                .setSuccess(accepted)
                .setStatus(accepted ? "VOID" : "FAILED")
                .setPaymentId(paymentId)
                .setProcessortransactionId(entity.getProcessorTransactionId())
                .setTimeStamp(System.currentTimeMillis())
                .setCreatedAt(LocalDate.now().toString())
                .setUpdatedAt(LocalDate.now().toString())
                .build();
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
