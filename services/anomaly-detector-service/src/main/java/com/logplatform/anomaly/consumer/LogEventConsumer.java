package com.logplatform.anomaly.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logplatform.anomaly.dto.LogEventDto;
import com.logplatform.anomaly.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LogEventConsumer {

    private final AnomalyDetectionService anomalyDetectionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.error-logs:error-logs}",
            groupId = "${spring.kafka.consumer.group-id:anomaly-detector}",
            concurrency = "3"
    )
    public void consumeErrorLogs(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            LogEventDto event = objectMapper.readValue(record.value(), LogEventDto.class);
            log.debug("Consumed error log from partition={} offset={} service={}",
                    record.partition(), record.offset(), event.getServiceName());
            anomalyDetectionService.processLogEvent(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process error log record offset={}: {}", record.offset(), e.getMessage());
            ack.acknowledge(); // avoid poison pill blocking
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.raw-logs:raw-logs}",
            groupId = "${spring.kafka.consumer.group-id:anomaly-detector}-volume",
            concurrency = "2"
    )
    public void consumeAllLogs(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            LogEventDto event = objectMapper.readValue(record.value(), LogEventDto.class);
            // Only volume-spike strategy runs on all logs to avoid redundant processing
            anomalyDetectionService.processLogEvent(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process raw log record offset={}: {}", record.offset(), e.getMessage());
            ack.acknowledge();
        }
    }
}
