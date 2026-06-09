package com.zaheudev.integration;

import com.zaheudev.integration.metrics.EventRecorder;
import com.zaheudev.integration.metrics.MetricsCollector;
import com.zaheudev.integration.risk.DeterministicRiskService;
import com.zaheudev.shared.avro.PaymentStatus;
import com.zaheudev.shared.dto.TokenStatus;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public abstract class BaseIT {

    protected static final Logger log = LoggerFactory.getLogger(BaseIT.class);

    @RegisterExtension
    static final EdaInfrastructure infra = new EdaInfrastructure();

    protected static ServiceManager services;
    protected static EventRecorder eventRecorder;
    protected static final MetricsCollector metrics = new MetricsCollector();
    protected static final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    protected static void startSystem() {
        stopSystem();
        services = new ServiceManager();
        services.startAll();
        eventRecorder = new EventRecorder();
    }

    protected static void stopSystem() {
        if (eventRecorder != null) { eventRecorder.stop(); eventRecorder = null; }
        if (services != null) { services.stopAll(); services = null; }
    }

    protected static String gatewayUrl() {
        return services.getGatewayUrl();
    }

    protected HttpResponse<String> postJson(String path, String body) throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(gatewayUrl() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> getJson(String path) throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder()
                .uri(URI.create(gatewayUrl() + path))
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    protected static String buildPaymentRequest(String merchantRef, String amount, String currency) {
        return """
        {
            "merchantRef": "%s",
            "amount": %s,
            "currency": "%s",
            "cardDetails": {
                "cardNumber": "4111111111111111",
                "expiryMonth": "12",
                "expiryYear": "2030",
                "cardHolderName": "Test User",
                "cvv": "123"
            }
        }
        """.formatted(merchantRef, amount, currency);
    }

    protected static void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
