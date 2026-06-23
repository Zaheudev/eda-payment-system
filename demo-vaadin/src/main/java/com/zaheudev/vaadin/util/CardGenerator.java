package com.zaheudev.vaadin.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

public final class CardGenerator {

    private static final SecureRandom RNG = new SecureRandom();

    private static final List<String> FIRST_NAMES = List.of(
            "Andrei", "Alexandru", "Ionut", "Mihai", "Andrei", "Radu", "Vlad", "Cristian",
            "Ioana", "Andreea", "Maria", "Elena", "Alexandra", "Ana", "Daniela", "Roxana");
    private static final List<String> LAST_NAMES = List.of(
            "Popescu", "Ionescu", "Popa", "Georgescu", "Stan", "Dumitrescu", "Radu", "Tabacu",
            "Mihai", "Stancu", "Vasile", "Dobre", "Marin", "Albu", "Gheorghe", "Nica");

    private CardGenerator() {}

    public static String generatePAN(String network, String cardType) {
        String net = network == null ? "VISA" : network.toUpperCase();
        boolean debit = "DEBIT".equalsIgnoreCase(cardType);
        int length = "AMEX".equals(net) ? 15 : 16;

        Integer[] pan = new Integer[length];
        pan[length - 1] = 0;
        for (int i = 0; i < length - 1; i++) {
            pan[i] = RNG.nextInt(10);
        }
        applyBin(pan, net, debit);
        int sum = luhnSum(pan);
        if (sum % 10 != 0) {
            pan[length - 1] = 10 - (sum % 10);
        }
        return toString(pan);
    }

    public static String generateCardholder() {
        return FIRST_NAMES.get(RNG.nextInt(FIRST_NAMES.size())) + " "
                + LAST_NAMES.get(RNG.nextInt(LAST_NAMES.size()));
    }

    public static String generateMerchantRef() {
        return "order-" + Long.toString(System.currentTimeMillis(), 36)
                + "-" + Integer.toString(RNG.nextInt(1296), 36);
    }

    public static String generateExpiry() {
        int month = RNG.nextInt(12) + 1;
        int year = LocalDate.now().getYear() + RNG.nextInt(6) + 1;
        return String.format("%02d/%d", month, year);
    }

    public static String generateCvv() {
        return String.format("%03d", RNG.nextInt(1000));
    }

    private static void applyBin(Integer[] pan, String network, boolean debit) {
        switch (network) {
            case "VISA" -> {
                pan[0] = 4;
                for (int i = 1; i < 5; i++) pan[i] = RNG.nextInt(10);
            }
            case "MASTERCARD" -> {
                pan[0] = 5;
                pan[1] = RNG.nextInt(5) + 1;
                for (int i = 2; i < 5; i++) pan[i] = RNG.nextInt(10);
            }
            case "AMEX" -> {
                pan[0] = 3;
                pan[1] = RNG.nextBoolean() ? 4 : 7;
                for (int i = 2; i < 5; i++) pan[i] = RNG.nextInt(10);
            }
            case "DISCOVER" -> {
                pan[0] = 6;
                for (int i = 1; i < 5; i++) pan[i] = RNG.nextInt(10);
            }
            default -> {
                pan[0] = 4;
                for (int i = 1; i < 5; i++) pan[i] = RNG.nextInt(10);
            }
        }
        pan[5] = debit ? 1 : randomNonOne();
    }

    private static int randomNonOne() {
        int d;
        do {
            d = RNG.nextInt(10);
        } while (d == 1);
        return d;
    }

    private static int luhnSum(Integer[] pan) {
        Integer[] reversed = reverse(pan);
        int sum = 0;
        for (int i = 0; i < reversed.length; i++) {
            int v = reversed[i];
            if (i % 2 == 1) {
                v = v * 2;
                if (v > 9) {
                    sum += v % 10 + 1;
                } else {
                    sum += v;
                }
            } else {
                sum += v;
            }
        }
        return sum;
    }

    private static Integer[] reverse(Integer[] pan) {
        Integer[] r = new Integer[pan.length];
        for (int i = 0; i < pan.length; i++) {
            r[i] = pan[pan.length - i - 1];
        }
        return r;
    }

    private static String toString(Integer[] pan) {
        StringBuilder sb = new StringBuilder();
        for (Integer n : pan) sb.append(n);
        return sb.toString();
    }
}
