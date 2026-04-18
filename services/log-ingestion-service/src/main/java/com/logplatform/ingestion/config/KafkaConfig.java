package com.logplatform.ingestion.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import com.logplatform.ingestion.model.LogEntry;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // strongest durability
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
public KafkaTemplate<String, String> kafkaTemplate() { // ← change this
    return new KafkaTemplate<>(producerFactory());
}

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic rawLogsTopic() {
        return TopicBuilder.name("raw-logs")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic errorLogsTopic() {
        return TopicBuilder.name("error-logs")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic anomalyEventsTopic() {
        return TopicBuilder.name("anomaly-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic alertsTopic() {
        return TopicBuilder.name("alerts")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
