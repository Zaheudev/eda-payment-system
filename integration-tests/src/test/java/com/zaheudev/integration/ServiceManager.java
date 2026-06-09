package com.zaheudev.integration;

import com.zaheudev.emulator.CardNetworkEmulatorApplication;
import com.zaheudev.gateway.PaymentGatewayApplication;
import com.zaheudev.integration.risk.DeterministicRiskConfig;
import com.zaheudev.risk.RiskFraudApplication;
import com.zaheudev.routing.RoutingServiceApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServiceManager {

    private static final Logger log = LoggerFactory.getLogger(ServiceManager.class);

    private ConfigurableApplicationContext gatewayCtx;
    private ConfigurableApplicationContext riskCtx;
    private ConfigurableApplicationContext routingCtx;
    private ConfigurableApplicationContext emulatorCtx;

    private String gatewayTestPort;

    private final Map<String, Object> commonProps;

    public ServiceManager() {
        commonProps = new HashMap<>();
        commonProps.put("spring.kafka.bootstrap-servers", EdaInfrastructure.getKafkaBootstrapServers());
        commonProps.put("spring.kafka.properties.schema.registry.url", EdaInfrastructure.getSchemaRegistryUrl());
        commonProps.put("spring.datasource.url", EdaInfrastructure.getPostgresJdbcUrl());
        commonProps.put("spring.datasource.username", EdaInfrastructure.getPostgresUsername());
        commonProps.put("spring.datasource.password", EdaInfrastructure.getPostgresPassword());
        commonProps.put("ctm.base.url", EdaInfrastructure.getCtmBaseUrl());
        commonProps.put("spring.jpa.hibernate.ddl-auto", "update");
        commonProps.put("spring.flyway.enabled", "false");
        commonProps.put("spring.kafka.producer.key-serializer", "org.apache.kafka.common.serialization.StringSerializer");
        commonProps.put("spring.kafka.producer.value-serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        commonProps.put("spring.kafka.consumer.key-deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        commonProps.put("spring.kafka.consumer.value-deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        commonProps.put("spring.kafka.properties.specific.avro.reader", "true");
        commonProps.put("spring.kafka.consumer.auto-offset-reset", "earliest");
    }

    public void startAll() {
        log.info("Starting payment-gateway...");
        gatewayCtx = boot(PaymentGatewayApplication.class, svcProps("gateway-group-it"));
        gatewayTestPort = gatewayCtx.getEnvironment().getProperty("local.server.port");
        log.info("Gateway running on port {}", gatewayTestPort);

        log.info("Starting risk-fraud...");
        Map<String, Object> riskProps = svcProps("risk-group-it");
        riskProps.put("test.deterministic.risk", "true");
        riskCtx = boot(RiskFraudApplication.class, riskProps, DeterministicRiskConfig.class);
        log.info("Risk-fraud started");

        log.info("Starting routing...");
        routingCtx = boot(RoutingServiceApplication.class, svcProps("routing-group-it"));
        log.info("Routing started");

        log.info("Starting card-network-emulator...");
        Map<String, Object> emulatorProps = svcProps("emulator-group-it");
        emulatorProps.put("cne.latency.min-ms", "10");
        emulatorProps.put("cne.latency.max-ms", "100");
        emulatorProps.put("cne.authorization-failure-percent", "0");
        emulatorCtx = boot(CardNetworkEmulatorApplication.class, emulatorProps);
        log.info("Emulator started");
    }

    private Map<String, Object> svcProps(String groupId) {
        Map<String, Object> props = new HashMap<>(commonProps);
        props.put("spring.kafka.consumer.group-id", groupId);
        props.put("server.port", 0);
        return props;
    }

    private ConfigurableApplicationContext boot(Class<?> appClass, Map<String, Object> props, Class<?>... extraSources) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(appClass).profiles("test");
        if (extraSources.length > 0) {
            builder = builder.sources(extraSources);
        }
        Map<String, Object> finalProps = new HashMap<>(props);
        builder.initializers(ctx -> ctx.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("test-override", finalProps)));
        return builder.run();
    }

    public void stopAll() {
        log.info("Stopping all service contexts...");
        if (emulatorCtx != null) { emulatorCtx.close(); emulatorCtx = null; }
        if (routingCtx != null) { routingCtx.close(); routingCtx = null; }
        if (riskCtx != null) { riskCtx.close(); riskCtx = null; }
        if (gatewayCtx != null) { gatewayCtx.close(); gatewayCtx = null; }
    }

    public void stopRouting() {
        log.info("Stopping routing context (simulating crash)...");
        if (routingCtx != null) { routingCtx.close(); routingCtx = null; }
    }

    public void startRouting() {
        log.info("Restarting routing context...");
        routingCtx = boot(RoutingServiceApplication.class, svcProps("routing-group-it-" + UUID.randomUUID()));
    }

    public void stopEmulator() {
        log.info("Stopping emulator context (simulating unavailability)...");
        if (emulatorCtx != null) { emulatorCtx.close(); emulatorCtx = null; }
    }

    public void startEmulator() {
        log.info("Restarting emulator context...");
        Map<String, Object> emulatorProps = svcProps("emulator-group-it-" + UUID.randomUUID());
        emulatorProps.put("cne.latency.min-ms", "10");
        emulatorProps.put("cne.latency.max-ms", "100");
        emulatorProps.put("cne.authorization-failure-percent", "0");
        emulatorCtx = boot(CardNetworkEmulatorApplication.class, emulatorProps);
    }

    public String getGatewayUrl() {
        return "http://localhost:" + gatewayTestPort;
    }

    public String getGatewayPort() {
        return gatewayTestPort;
    }

    public int getEmulatorPort() {
        return emulatorCtx != null
                ? Integer.parseInt(emulatorCtx.getEnvironment().getProperty("local.server.port", "0")) : 0;
    }

    public int getRoutingPort() {
        return routingCtx != null
                ? Integer.parseInt(routingCtx.getEnvironment().getProperty("local.server.port", "0")) : 0;
    }

    public int getRiskPort() {
        return riskCtx != null
                ? Integer.parseInt(riskCtx.getEnvironment().getProperty("local.server.port", "0")) : 0;
    }

    public boolean isRoutingAlive() {
        return routingCtx != null;
    }

    public boolean isEmulatorAlive() {
        return emulatorCtx != null;
    }
}
