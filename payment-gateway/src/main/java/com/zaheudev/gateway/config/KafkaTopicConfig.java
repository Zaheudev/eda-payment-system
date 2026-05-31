package com.zaheudev.gateway.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentRequests() {
        return TopicBuilder.name("payment-requests").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic riskAssessed() {
        return TopicBuilder.name("risk-assessed").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentRejected() {
        return TopicBuilder.name("payment-rejected").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic routingCompleted() {
        return TopicBuilder.name("routing-completed").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic authorizationCompleted() {
        return TopicBuilder.name("authorization-completed").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic captureRequests() {
        return TopicBuilder.name("capture-requests").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic captureCompleted() {
        return TopicBuilder.name("capture-completed").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic refundRequests() {
        return TopicBuilder.name("refund-requests").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic refundCompleted() {
        return TopicBuilder.name("refund-completed").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic voidRequests() {
        return TopicBuilder.name("void-requests").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic voidCompleted() {
        return TopicBuilder.name("void-completed").partitions(3).replicas(1).build();
    }
}
