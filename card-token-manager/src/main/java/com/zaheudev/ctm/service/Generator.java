package com.zaheudev.ctm.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class Generator {
    private final SecureRandom random = new SecureRandom();
    /**
        This is used for generating the mask for raw PAN. This mask pass the validation of luhn algortihm
        With this system we can generate many raw PAN for testing diverse payemnts, but needs the generating
        logic modificated to match the pattern regex for determinating card network, debit or credit card etc.
     */
    public String generatePAN(Integer length) {
        Integer[] pan = new Integer[length];
        pan[length-1] = 0;
        for(int i = 0; i < length-1; i++){
            pan[i] = random.nextInt(10);
        }
        int sum = calculateLuhnSum(pan);
        if(sum % 10 != 0){
            int rest = sum % 10;
            pan[length-1] = 10-rest;
        }
        return toString(pan);
    }

    public String encryptPAN(String pan, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        String iv = generateIV();
        GCMParameterSpec spec = new GCMParameterSpec(128, Base64.getDecoder().decode(iv));
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] encrypted = cipher.doFinal(pan.getBytes());
        return iv+":"+Base64.getEncoder().encodeToString(encrypted);
    }

    private String generateIV(){
        return Base64.getEncoder().encodeToString(random.generateSeed(12));
    }

    public String decryptPAN(String encryptedPAN, SecretKey key) throws Exception{
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        String extractedIV = encryptedPAN.split(":")[0];
        GCMParameterSpec spec = new GCMParameterSpec(128, Base64.getDecoder().decode(extractedIV));
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedPAN.split(":")[1])));
    }

    private Integer calculateLuhnSum(Integer[] pan) {
        Integer[] reversedPan = reverse(pan);
        Integer sum = 0;
        for(int i = 0; i<reversedPan.length; i++){
            int multiplied = 0;
            if(i % 2 == 1){
                multiplied = reversedPan[i] * 2;
                if(multiplied > 9){
                    sum += multiplied % 10 + 1;
                }else{
                    sum += multiplied;
                }
            }else {
                sum += reversedPan[i];
            }
        }
        return sum;
    }

    private Integer[] reverse(Integer[] pan){
        Integer[] reversedPan = new Integer[pan.length];
        for(int i = 0; i < pan.length; i++){
            reversedPan[i] = pan[pan.length - i - 1];
        }
        return reversedPan;
    }

    private String toString(Integer[] pan){
        StringBuilder sb = new StringBuilder();
        for(Integer n : pan){
            sb.append(n);
        }
        return sb.toString();
    }
}
