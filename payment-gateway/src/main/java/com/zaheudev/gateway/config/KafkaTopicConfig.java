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

    @Bean
    public NewTopic paymentRequestsDlt() {
        return TopicBuilder.name("payment-requests.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic riskAssessedDlt() {
        return TopicBuilder.name("risk-assessed.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentRejectedDlt() {
        return TopicBuilder.name("payment-rejected.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic routingCompletedDlt() {
        return TopicBuilder.name("routing-completed.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic authorizationCompletedDlt() {
        return TopicBuilder.name("authorization-completed.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic captureRequestsDlt() {
        return TopicBuilder.name("capture-requests.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic captureCompletedDlt() {
        return TopicBuilder.name("capture-completed.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic refundRequestsDlt() {
        return TopicBuilder.name("refund-requests.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic refundCompletedDlt() {
        return TopicBuilder.name("refund-completed.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic voidRequestsDlt() {
        return TopicBuilder.name("void-requests.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic voidCompletedDlt() {
        return TopicBuilder.name("void-completed.DLT").partitions(3).replicas(1).build();
    }
}
