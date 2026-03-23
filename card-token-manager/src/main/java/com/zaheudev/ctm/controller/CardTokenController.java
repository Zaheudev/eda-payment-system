package com.zaheudev.ctm.controller;

import com.zaheudev.ctm.dto.DetokenizeResponse;
import com.zaheudev.ctm.dto.TokenizeResponse;
import com.zaheudev.ctm.service.CardTokenService;
import com.zaheudev.shared.avro.CardDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CardTokenController {
    @Autowired
    CardTokenService cardTokenService;

    @PostMapping("/api/v1/tokenize")
    public ResponseEntity<TokenizeResponse> tokenize(@RequestBody CardDetails cardDetails){
        try{
            TokenizeResponse response = cardTokenService.tokenize(cardDetails, cardDetails.getExpiryMonth().toString(), cardDetails.getExpiryYear().toString());
            return new ResponseEntity<>(response, null, 201);
        }catch (Exception e){
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(null, null, 500);
        }
    }

    @GetMapping("/api/v1/detokenize")
    public ResponseEntity<DetokenizeResponse> detokenize(@RequestParam("tokenRef") String tokenRef){
        try{
            DetokenizeResponse response = cardTokenService.detokenize(tokenRef);
            return new ResponseEntity<>(response, null, 200);
        }catch (Exception e){
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(null, null, 500);
        }
    }
}
