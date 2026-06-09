package com.zaheudev.load.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class PaymentFeeder implements Iterator<Map<String, Object>> {

    private static final String[] CURRENCIES = {"USD", "EUR", "GBP", "RON"};
    private static final String[] NETWORKS = {"VISA", "MASTERCARD", "AMEX"};
    private static final String[] HOLDERS = {"John Doe", "Jane Smith", "Alex Johnson", "Maria Garcia", "Sam Brown"};

    private static final Map<String, String[]> CARD_NUMBERS = Map.of(
            "VISA", new String[]{"4111111111111111", "4530001111111111"},
            "MASTERCARD", new String[]{"5111111111111111", "5200001111111111"},
            "AMEX", new String[]{"371111111111111", "341111111111111"}
    );

    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public Map<String, Object> next() {
        long idx = counter.incrementAndGet();
        var rng = ThreadLocalRandom.current();

        String network = NETWORKS[rng.nextInt(NETWORKS.length)];
        String[] cards = CARD_NUMBERS.get(network);
        String cardNumber = cards[rng.nextInt(cards.length)];

        String merchantRef = "load-" + idx;
        BigDecimal amount = BigDecimal.valueOf(rng.nextDouble(5.0, 500.0))
                .setScale(2, RoundingMode.HALF_UP);
        String currency = CURRENCIES[rng.nextInt(CURRENCIES.length)];
        String expiryMonth = String.format("%02d", rng.nextInt(1, 13));
        String expiryYear = String.valueOf(rng.nextInt(2026, 2032));
        String cardHolderName = HOLDERS[rng.nextInt(HOLDERS.length)];
        String cvv = String.format("%03d", rng.nextInt(100, 1000));

        String jsonBody = String.format("""
                {
                    "merchantRef": "%s",
                    "amount": %s,
                    "currency": "%s",
                    "cardDetails": {
                        "cardNumber": "%s",
                        "expiryMonth": "%s",
                        "expiryYear": "%s",
                        "cardHolderName": "%s",
                        "cvv": "%s"
                    }
                }""", merchantRef, amount, currency, cardNumber, expiryMonth, expiryYear, cardHolderName, cvv);

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("merchantRef", merchantRef);
        record.put("amount", amount.toString());
        record.put("currency", currency);
        record.put("cardNetwork", network);
        record.put("jsonBody", jsonBody);
        return record;
    }
}
