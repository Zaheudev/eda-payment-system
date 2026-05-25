package com.zaheudev.emulator.client;

import com.zaheudev.shared.dto.CardTokenMetadata;
import com.zaheudev.shared.dto.TokenStatus;
import org.antlr.v4.runtime.Token;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CardMetadataClient {
    RestClient restClient = RestClient.create();

    @Value("${ctm.base.url}")
    private String BASE_URL;

    public CardTokenMetadata getMetadata(String tokenRef) {
         ResponseEntity<CardTokenMetadata> response = this.restClient.get()
                .uri(BASE_URL + "/api/v1/" + tokenRef)
                .retrieve()
                .toEntity(CardTokenMetadata.class);
         return response.getBody();
    }

    public TokenStatus getTokenStatus(String tokenRef){
        ResponseEntity<TokenStatus> response = this.restClient.get()
                .uri(BASE_URL + "/api/v1/" + tokenRef + "/status")
                .retrieve()
                .toEntity(TokenStatus.class);
        return response.getBody();
    }
}
