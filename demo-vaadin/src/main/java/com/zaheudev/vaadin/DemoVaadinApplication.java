package com.zaheudev.vaadin;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@Push
@Theme("demo")
public class DemoVaadinApplication implements AppShellConfigurator {
    public static void main(String[] args) {
        SpringApplication.run(DemoVaadinApplication.class, args);
    }
}
