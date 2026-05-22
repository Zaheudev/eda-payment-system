package com.zaheudev.gateway.config;

import com.zaheudev.shared.avro.CaptureRequestedEvent;
import com.zaheudev.shared.avro.PaymentRequestedEvent;
import com.zaheudev.shared.avro.RefundRequestedEvent;
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
    public ProducerFactory<String, Object> producerConfig() {
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
    public ProducerFactory<String, PaymentRequestedEvent> paymentRequestedProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig().getConfigurationProperties());
    }

    @Bean
    public KafkaTemplate<String, PaymentRequestedEvent> paymentRequestedKafkaTemplate() {
        return new KafkaTemplate<>(paymentRequestedProducerFactory());
    }

    @Bean
    public ProducerFactory<String, CaptureRequestedEvent> captureRequestedProducerFactory(){
        return new DefaultKafkaProducerFactory<>(producerConfig().getConfigurationProperties());
    }

    @Bean
    public KafkaTemplate<String, CaptureRequestedEvent> captureRequestedKafkaTemplate(){
        return new KafkaTemplate<>(captureRequestedProducerFactory());
    }

    @Bean
    public ProducerFactory<String, RefundRequestedEvent> refundRequestedProducerFactory(){
        return new DefaultKafkaProducerFactory<>(producerConfig().getConfigurationProperties());
    }

    @Bean
    public KafkaTemplate<String, RefundRequestedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(refundRequestedProducerFactory());
    }
}
