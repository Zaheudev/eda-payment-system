package com.zaheudev.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class RiskFraudApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiskFraudApplication.class, args);
    }
}
