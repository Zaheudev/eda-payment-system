package com.zaheudev.ctm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratorTest {

    private Generator generator;
    private SecretKeySpec key;

    @BeforeEach
    void setUp() {
        generator = new Generator();
        byte[] keyBytes = "1234567890123456".getBytes(StandardCharsets.UTF_8);
        key = new SecretKeySpec(keyBytes, "AES");
    }

    @Test
    void generatePANShouldProduceCorrectLength() {
        String pan = generator.generatePAN(16);
        assertThat(pan).hasSize(16);
    }

    @Test
    void generatePANShouldBeLuhnValid() {
        for (int i = 0; i < 10; i++) {
            String pan = generator.generatePAN(16);
            assertThat(isLuhnValid(pan))
                    .as("PAN %s should be Luhn-valid", pan)
                    .isTrue();
        }
    }

    @Test
    void encryptDecryptRoundTripShouldRecoverOriginal() throws Exception {
        String original = "4111111111111111";
        String encrypted = generator.encryptPAN(original, key);
        assertThat(encrypted).isNotNull().contains(":");

        String decrypted = generator.decryptPAN(encrypted, key);
        assertThat(decrypted).isEqualTo(original);
    }

    private boolean isLuhnValid(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(number.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
