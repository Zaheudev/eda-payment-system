package com.zaheudev.emulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class CardNetworkEmulatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardNetworkEmulatorApplication.class, args);
    }
}
