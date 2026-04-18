package com.logplatform.rca.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logplatform.rca.dto.AnomalyEventDto;
import com.logplatform.rca.service.RcaEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AnomalyEventConsumer {

    private final RcaEngineService rcaEngineService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.anomaly-events:anomaly-events}",
            groupId = "${spring.kafka.consumer.group-id:rca-engine}",
            concurrency = "2"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            AnomalyEventDto anomaly = objectMapper.readValue(record.value(), AnomalyEventDto.class);
            log.info("Received anomaly event id={} service={} type={}",
                    anomaly.getId(), anomaly.getServiceName(), anomaly.getAnomalyType());

            // Async — does not block Kafka consumer thread
            rcaEngineService.triggerRca(anomaly);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process anomaly event at offset={}: {}", record.offset(), e.getMessage());
            ack.acknowledge(); // prevent poison pill
        }
    }
}
