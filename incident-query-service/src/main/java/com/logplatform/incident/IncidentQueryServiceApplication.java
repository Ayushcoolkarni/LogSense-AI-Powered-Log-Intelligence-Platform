package com.logplatform.incident;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IncidentQueryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IncidentQueryServiceApplication.class, args);
    }
}