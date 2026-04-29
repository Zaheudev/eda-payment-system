package com.zaheudev.gateway.client;

import com.zaheudev.gateway.dto.CardDetails;
import com.zaheudev.shared.dto.CardTokenMetadata;
import com.zaheudev.shared.dto.DetokenizeResponse;
import com.zaheudev.shared.dto.TokenizeRequest;
import com.zaheudev.shared.dto.TokenizeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TokenizerClient {
    RestClient restClient = RestClient.create();
    @Value("${ctm.base.url}")
    private String BASE_URL;
    public TokenizeResponse tokenize(CardDetails cardDetails) {
        TokenizeRequest req = TokenizeRequest.builder()
                .cardNumber(cardDetails.getCardNumber())
                .cvv(cardDetails.getCvv())
                .expiryMonth(cardDetails.getExpiryMonth())
                .expiryYear(cardDetails.getExpiryYear())
                .cardHolderName(cardDetails.getCardHolderName())
                .build();
        ResponseEntity<TokenizeResponse> response = this.restClient.post()
                .uri(BASE_URL+"/api/v1/tokenize")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .toEntity(TokenizeResponse.class);

        return response.getBody();
    }

    public DetokenizeResponse detokenize(String tokenRef) {
        ResponseEntity<DetokenizeResponse> response = this.restClient.get()
                .uri(BASE_URL+"/api/v1/" + tokenRef + "/detokenize")
                .retrieve()
                .toEntity(DetokenizeResponse.class);
        return response.getBody();
    }

    public CardTokenMetadata getMetadata(String tokenRef) {
        ResponseEntity<CardTokenMetadata> response = this.restClient.get()
                .uri(BASE_URL+"/api/v1/" + tokenRef)
                .retrieve()
                .toEntity(CardTokenMetadata.class);
        return response.getBody();
    }
}
