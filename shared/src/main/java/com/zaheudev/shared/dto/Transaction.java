package com.zaheudev.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private String id;
    private String transactionReference;
    private BigDecimal amount;
    private String merchantId;
    private CardTokenMetadata cardInfo;
    private String merchantName;
    private String merchantCategoryCode;
    private String userId;
    private String userEmail;
    private String userIpAddress;
    private String userDeviceId;
    private String billingAddress;
    private String shippingAddress;
    private LocalDateTime transactionTime;
    private String transactionChannel; // WEB, MOBILE, IN_STORE, etc.
    private boolean isRecurring;
    private int previousChargebacks;
    private int previousSuccessfulTransactions;
    private int previousFailedTransactions;
}
