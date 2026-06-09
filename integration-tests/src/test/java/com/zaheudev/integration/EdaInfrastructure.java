package com.zaheudev.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class EdaInfrastructure implements BeforeAllCallback, AfterAllCallback {

    private static final Logger log = LoggerFactory.getLogger(EdaInfrastructure.class);

    private static final Network network = Network.newNetwork();

    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
            .withNetwork(network)
            .withNetworkAliases("kafka");

    static final GenericContainer<?> schemaRegistry = new GenericContainer<>(
            DockerImageName.parse("confluentinc/cp-schema-registry:7.6.0"))
            .withNetwork(network)
            .withNetworkAliases("schema-registry")
            .withExposedPorts(8081)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "kafka:9092")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .dependsOn(kafka)
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16"))
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("payment-gateway")
            .withUsername("zaheu")
            .withPassword("1234");

    static WireMockServer wireMock;

    static volatile boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (started) return;
        started = true;

        log.info("Starting Kafka container...");
        kafka.start();
        log.info("Starting Schema Registry container...");
        schemaRegistry.start();
        log.info("Starting PostgreSQL container...");
        postgres.start();
        log.info("Starting WireMock server...");
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());
        stubCtmEndpoints();

        log.info("Infrastructure ready. Kafka: {}, SchemaRegistry: {}, Postgres: {}, WireMock: {}",
                getKafkaBootstrapServers(), getSchemaRegistryUrl(), getPostgresJdbcUrl(), getCtmBaseUrl());
    }

    @Override
    public void afterAll(ExtensionContext context) {
    }

    public static String getKafkaBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    public static String getSchemaRegistryUrl() {
        return "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getMappedPort(8081);
    }

    public static String getPostgresJdbcUrl() {
        return postgres.getJdbcUrl();
    }

    public static String getPostgresUsername() {
        return postgres.getUsername();
    }

    public static String getPostgresPassword() {
        return postgres.getPassword();
    }

    public static String getCtmBaseUrl() {
        return "http://localhost:" + wireMock.port();
    }

    private static void stubCtmEndpoints() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v1/tokenize"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                            "tokenRef": "tkn-mock-001",
                            "tokenValue": "tk-mock-token-value-abc123",
                            "bin": "411111",
                            "lastFour": "1111",
                            "cardType": "CREDIT",
                            "cardNetwork": "VISA",
                            "cardholderName": "Test User",
                            "expiryMonth": "12",
                            "expiryYear": "2030",
                            "status": "ACTIVE"
                        }
                        """)));

        wireMock.stubFor(get(urlPathMatching("/api/v1/((?!status|detokenize).)+"))
                .atPriority(2)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                            "bin": "411111",
                            "lastFour": "1111",
                            "cardType": "CREDIT",
                            "cardNetwork": "VISA",
                            "cardholderName": "Test User",
                            "expiryMonth": "12",
                            "expiryYear": "2030",
                            "status": "ACTIVE"
                        }
                        """)));

        wireMock.stubFor(get(urlPathMatching("/api/v1/.+/status"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("\"ACTIVE\"")));

        wireMock.stubFor(get(urlPathMatching("/api/v1/.+/detokenize"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                            "cardNumber": "4111111111111111",
                            "expiryMonth": "12",
                            "expiryYear": "2030",
                            "cardHolderName": "Test User"
                        }
                        """)));
    }
}
