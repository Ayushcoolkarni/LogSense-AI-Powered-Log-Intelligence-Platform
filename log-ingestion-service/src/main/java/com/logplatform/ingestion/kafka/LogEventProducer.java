package com.logplatform.ingestion.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logplatform.ingestion.model.LogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class LogEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.raw-logs:raw-logs}")
    private String rawLogsTopic;

    @Value("${kafka.topics.error-logs:error-logs}")
    private String errorLogsTopic;

    @Async
    public void publishLogEntry(LogEntry entry) {
        try {
            String payload = objectMapper.writeValueAsString(entry);
            String key = entry.getServiceName() + "-" + entry.getLogLevel();

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(rawLogsTopic, key, payload);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish log entry id={} to topic={}: {}",
                            entry.getId(), rawLogsTopic, ex.getMessage());
                } else {
                    log.debug("Published log entry id={} to topic={} partition={} offset={}",
                            entry.getId(), rawLogsTopic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

            // Route ERROR/WARN logs to dedicated topic for faster anomaly detection
            if ("ERROR".equals(entry.getLogLevel()) || "WARN".equals(entry.getLogLevel())) {
                publishToErrorTopic(entry, payload);
            }

        } catch (Exception e) {
            log.error("Serialization error for log entry id={}: {}", entry.getId(), e.getMessage());
        }
    }

    private void publishToErrorTopic(LogEntry entry, String payload) {
        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(errorLogsTopic, entry.getServiceName(), payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish ERROR log id={} to error topic: {}",
                        entry.getId(), ex.getMessage());
            }
        });
    }

    @Async
    public void publishBatch(java.util.List<LogEntry> entries) {
        entries.forEach(this::publishLogEntry);
        log.info("Batch published {} log entries to Kafka", entries.size());
    }
}
