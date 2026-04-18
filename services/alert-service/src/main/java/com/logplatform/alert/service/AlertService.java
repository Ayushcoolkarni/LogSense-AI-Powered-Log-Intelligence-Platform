package com.logplatform.alert.service;

import com.logplatform.alert.channel.NotificationChannel;
import com.logplatform.alert.dto.AnomalyEventDto;
import com.logplatform.alert.model.Alert;
import com.logplatform.alert.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final List<NotificationChannel> channels;

    private static final int MAX_RETRIES = 3;

    @Transactional
    public void processAnomaly(AnomalyEventDto anomaly) {
        // Deduplicate: don't re-alert for same anomaly
        boolean exists = alertRepository.existsByAnomalyIdAndStatusNot(
                anomaly.getId(), Alert.AlertStatus.FAILED);
        if (exists) {
            log.debug("Alert already exists for anomalyId={}, skipping", anomaly.getId());
            return;
        }

        Alert.AlertSeverity severity = mapSeverity(anomaly.getSeverity());

        Alert alert = Alert.builder()
                .anomalyId(anomaly.getId())
                .serviceName(anomaly.getServiceName())
                .anomalyType(anomaly.getAnomalyType())
                .severity(severity)
                .message(anomaly.getDescription())
                .status(Alert.AlertStatus.PENDING)
                .build();

        Alert saved = alertRepository.save(alert);
        dispatchAlert(saved);
    }

    @Transactional
    public void dispatchAlert(Alert alert) {
        List<String> channelsSent = new ArrayList<>();
        boolean anySuccess = false;

        for (NotificationChannel channel : channels) {
            if (!channel.supports(alert.getSeverity())) continue;
            try {
                channel.send(alert);
                channelsSent.add(channel.channelName());
                anySuccess = true;
                log.info("Alert id={} sent via channel={}", alert.getId(), channel.channelName());
            } catch (Exception e) {
                log.error("Channel {} failed for alertId={}: {}", channel.channelName(), alert.getId(), e.getMessage());
            }
        }

        alert.setChannelsSent(channelsSent);
        alert.setSentAt(Instant.now());
        alert.setStatus(anySuccess ? Alert.AlertStatus.SENT : Alert.AlertStatus.FAILED);
        alertRepository.save(alert);
    }

    // Retry failed alerts every 5 minutes
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void retryFailedAlerts() {
        List<Alert> failed = alertRepository.findByStatusAndRetryCountLessThan(
                Alert.AlertStatus.FAILED, MAX_RETRIES);

        if (!failed.isEmpty()) {
            log.info("Retrying {} failed alerts", failed.size());
        }

        for (Alert alert : failed) {
            alert.setRetryCount(alert.getRetryCount() + 1);
            alertRepository.save(alert);
            dispatchAlert(alert);
        }
    }

    @Transactional
    public Alert acknowledge(String alertId, String acknowledgedBy) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alert.setStatus(Alert.AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(acknowledgedBy);
        alert.setAcknowledgedAt(Instant.now());
        return alertRepository.save(alert);
    }

    private Alert.AlertSeverity mapSeverity(String s) {
        return switch (s.toUpperCase()) {
            case "CRITICAL" -> Alert.AlertSeverity.CRITICAL;
            case "HIGH" -> Alert.AlertSeverity.HIGH;
            case "MEDIUM" -> Alert.AlertSeverity.MEDIUM;
            default -> Alert.AlertSeverity.LOW;
        };
    }
}
