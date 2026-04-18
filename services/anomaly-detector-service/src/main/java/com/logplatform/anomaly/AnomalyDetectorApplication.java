package com.logplatform.anomaly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableScheduling
public class AnomalyDetectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnomalyDetectorApplication.class, args);
    }
}
