package com.zaheudev.emulator.config;

import com.zaheudev.shared.avro.CaptureCompletedEvent;
import com.zaheudev.shared.avro.AuthorizationCompletedEvent;
import com.zaheudev.shared.avro.RefundCompletedEvent;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean
    public ProducerFactory<String, AuthorizationCompletedEvent> producerFactoryConfig() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        configProps.put("schema.registry.url", schemaRegistryUrl);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public ProducerFactory<String, AuthorizationCompletedEvent> producerFactoryAuthorizationCompleted() {
        return new DefaultKafkaProducerFactory<>(producerFactoryConfig().getConfigurationProperties());
    }

    @Bean
    public KafkaTemplate<String, AuthorizationCompletedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactoryAuthorizationCompleted());
    }

    @Bean
    public ProducerFactory<String, CaptureCompletedEvent> producerFactoryCapturedCompleted() {
        return new DefaultKafkaProducerFactory<>(producerFactoryConfig().getConfigurationProperties());
    }

    @Bean
    public KafkaTemplate<String, CaptureCompletedEvent> kafkaTemplateCapturedCompleted() {
        return new KafkaTemplate<>(producerFactoryCapturedCompleted());
    }

    @Bean
    public ProducerFactory<String, RefundCompletedEvent> producerFactoryRefundCompleted() {
        return new DefaultKafkaProducerFactory<>(producerFactoryConfig().getConfigurationProperties());
    }

    @Bean
    public KafkaTemplate<String, RefundCompletedEvent> kafkaTemplateRefundCompleted() {
        return new KafkaTemplate<>(producerFactoryRefundCompleted());
    }
}
