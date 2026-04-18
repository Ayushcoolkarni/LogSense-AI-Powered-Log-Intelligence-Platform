package com.logplatform.anomaly.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logplatform.anomaly.dto.LogEventDto;
import com.logplatform.anomaly.model.Anomaly;
import com.logplatform.anomaly.repository.AnomalyRepository;
import com.logplatform.anomaly.strategy.AnomalyDetectionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final List<AnomalyDetectionStrategy> strategies;
    private final AnomalyRepository anomalyRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String ANOMALY_EVENTS_TOPIC = "anomaly-events";

    @Transactional
    public void processLogEvent(LogEventDto event) {
        for (AnomalyDetectionStrategy strategy : strategies) {
            try {
                Optional<Anomaly> detected = strategy.analyze(event);
                detected.ifPresent(anomaly -> {
                    Anomaly saved = anomalyRepository.save(anomaly);
                    publishAnomalyEvent(saved);
                    log.info("Anomaly saved id={} type={} service={} severity={}",
                            saved.getId(), saved.getAnomalyType(), saved.getServiceName(), saved.getSeverity());
                });
            } catch (Exception e) {
                log.error("Strategy {} threw exception for event {}: {}",
                        strategy.strategyName(), event.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public Anomaly updateStatus(String anomalyId, Anomaly.AnomalyStatus newStatus, String resolvedBy) {
        Anomaly anomaly = anomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found: " + anomalyId));
        anomaly.setStatus(newStatus);
        if (newStatus == Anomaly.AnomalyStatus.RESOLVED || newStatus == Anomaly.AnomalyStatus.FALSE_POSITIVE) {
            anomaly.setResolvedBy(resolvedBy);
            anomaly.setResolvedAt(Instant.now());
        }
        return anomalyRepository.save(anomaly);
    }

    public Map<String, Object> getDashboardStats() {
        Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        long openCount = anomalyRepository.countByStatusAndDetectedAtAfter(Anomaly.AnomalyStatus.OPEN, oneDayAgo);

        Map<String, Long> byService = new HashMap<>();
        anomalyRepository.countOpenAnomaliesByService()
                .forEach(row -> byService.put((String) row[0], (Long) row[1]));

        Map<String, Long> byType = new HashMap<>();
        anomalyRepository.countByAnomalyType()
                .forEach(row -> byType.put(row[0].toString(), (Long) row[1]));

        Map<String, Object> stats = new HashMap<>();
        stats.put("openAnomaliesLast24h", openCount);
        stats.put("byService", byService);
        stats.put("byType", byType);
        return stats;
    }

    private void publishAnomalyEvent(Anomaly anomaly) {
        try {
            String payload = objectMapper.writeValueAsString(anomaly);
            kafkaTemplate.send(ANOMALY_EVENTS_TOPIC, anomaly.getServiceName(), payload);
        } catch (Exception e) {
            log.error("Failed to publish anomaly event id={}: {}", anomaly.getId(), e.getMessage());
        }
    }
}
