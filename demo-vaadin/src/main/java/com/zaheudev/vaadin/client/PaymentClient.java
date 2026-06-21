package com.zaheudev.vaadin.client;

import com.zaheudev.vaadin.model.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(@Value("${payment.gateway.url}") String gatewayUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(gatewayUrl)
                .build();
    }

    public PaymentResponse createPayment(Map<String, Object> request) {
        return restClient.post()
                .uri("/api/v1/payments")
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);
    }

    public PaymentResponse capture(String paymentId) {
        return restClient.post()
                .uri("/api/v1/capture/{paymentId}", paymentId)
                .retrieve()
                .body(PaymentResponse.class);
    }

    public PaymentResponse refund(String paymentId, Map<String, Object> request) {
        return restClient.post()
                .uri("/api/v1/refund/{paymentId}", paymentId)
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);
    }

    public PaymentResponse voidPayment(String paymentId) {
        return restClient.post()
                .uri("/api/v1/void/{paymentId}", paymentId)
                .retrieve()
                .body(PaymentResponse.class);
    }

    public List<PaymentResponse> fetchAllPayments(int limit) {
        return restClient.get()
                .uri("/api/v1/payments?limit={limit}", limit)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public PaymentResponse fetchPayment(String paymentId) {
        return restClient.get()
                .uri("/api/v1/payments/{paymentId}", paymentId)
                .retrieve()
                .body(PaymentResponse.class);
    }
}
