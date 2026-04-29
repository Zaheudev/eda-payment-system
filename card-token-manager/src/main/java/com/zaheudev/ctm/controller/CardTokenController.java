package com.zaheudev.ctm.controller;

import com.zaheudev.shared.dto.CardTokenMetadata;
import com.zaheudev.shared.dto.DetokenizeResponse;
import com.zaheudev.shared.dto.TokenizeRequest;
import com.zaheudev.shared.dto.TokenizeResponse;
import com.zaheudev.ctm.service.CardTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CardTokenController {
    @Autowired
    CardTokenService cardTokenService;

    @PostMapping("/api/v1/tokenize")
    public ResponseEntity<TokenizeResponse> tokenize(@RequestBody TokenizeRequest cardDetails){
        try{
            TokenizeResponse response = cardTokenService.tokenize(cardDetails);
            return new ResponseEntity<>(response, null, 201);
        }catch (Exception e){
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(null, null, 500);
        }
    }

    @GetMapping("/api/v1/{tokenRef}/detokenize")
    public ResponseEntity<DetokenizeResponse> detokenize(@PathVariable String tokenRef){
        try{
            DetokenizeResponse response = cardTokenService.detokenize(tokenRef);
            return new ResponseEntity<>(response, null, 200);
        }catch (Exception e){
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(null, null, 500);
        }
    }

    @GetMapping("/api/v1/{tokenRef}")
    public ResponseEntity<CardTokenMetadata> getMetadata(@PathVariable String tokenRef){
        return ResponseEntity.ok(cardTokenService.getMetadata(tokenRef));
    }
}
