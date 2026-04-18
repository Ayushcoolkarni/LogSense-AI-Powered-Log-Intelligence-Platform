package com.logplatform.alert.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logplatform.alert.dto.AnomalyEventDto;
import com.logplatform.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AnomalyConsumer {

    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.anomaly-events:anomaly-events}",
            groupId = "${spring.kafka.consumer.group-id:alert-service}"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            AnomalyEventDto anomaly = objectMapper.readValue(record.value(), AnomalyEventDto.class);
            log.info("Alert service received anomaly id={} service={}", anomaly.getId(), anomaly.getServiceName());
            alertService.processAnomaly(anomaly);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Alert consumer failed at offset={}: {}", record.offset(), e.getMessage());
            ack.acknowledge();
        }
    }
}
