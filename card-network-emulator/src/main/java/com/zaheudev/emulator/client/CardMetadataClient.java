package com.zaheudev.emulator.client;

import com.zaheudev.shared.dto.CardTokenMetadata;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CardMetadataClient {
    RestClient restClient = RestClient.create();

    public CardTokenMetadata getMetadata(String tokenRef) {
        return this.restClient.get()
                .uri("http://localhost:8084/api/v1/" + tokenRef)
                .retrieve()
                .toEntity(CardTokenMetadata.class)
                .getBody();
    }

}
