package com.zaheudev.ctm.controller;

import com.zaheudev.shared.dto.*;
import com.zaheudev.ctm.service.CardTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
public class CardTokenController {
    @Autowired
    private CardTokenService cardTokenService;

    @PostMapping("/api/v1/tokenize")
    public ResponseEntity<?> tokenize(@RequestBody TokenizeRequest cardDetails) {
        try {
            TokenizeResponse response = cardTokenService.tokenize(cardDetails);
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Tokenize failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Tokenization failed"));
        }
    }

    @GetMapping("/api/v1/{tokenRef}/detokenize")
    public ResponseEntity<?> detokenize(@PathVariable String tokenRef) {
        try {
            DetokenizeResponse response = cardTokenService.detokenize(tokenRef);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Detokenize failed for tokenRef: {}", tokenRef, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Detokenization failed"));
        }
    }

    @GetMapping("/api/v1/{tokenRef}")
    public ResponseEntity<CardTokenMetadata> getMetadata(@PathVariable String tokenRef){
        return ResponseEntity.ok(cardTokenService.getMetadata(tokenRef));
    }

    @GetMapping("/api/v1/{tokenRef}/status")
    public ResponseEntity<TokenStatus> getStatus(@PathVariable String tokenRef){
        return ResponseEntity.ok(cardTokenService.getStatus(tokenRef));
    }
}
