package com.logplatform.anomaly.strategy;

import com.logplatform.anomaly.dto.LogEventDto;
import com.logplatform.anomaly.model.Anomaly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Detects keywords in ERROR logs indicating a service is down or unreachable.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ServiceUnavailableStrategy implements AnomalyDetectionStrategy {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String COOLDOWN_KEY_PREFIX = "svc_unavail_cooldown:";
    private static final int COOLDOWN_SECONDS = 300; // 5 min cooldown per service

    private static final List<String> UNAVAILABILITY_KEYWORDS = List.of(
            "connection refused", "connection timeout", "service unavailable",
            "503", "host unreachable", "no route to host", "circuit breaker open",
            "upstream connect error", "econnrefused"
    );

    @Override
    public Optional<Anomaly> analyze(LogEventDto event) {
        if (!"ERROR".equals(event.getLogLevel())) {
            return Optional.empty();
        }

        String msg = event.getMessage() == null ? "" : event.getMessage().toLowerCase();
        boolean isUnavailable = UNAVAILABILITY_KEYWORDS.stream().anyMatch(msg::contains);

        if (!isUnavailable) {
            return Optional.empty();
        }

        // Cooldown: avoid spamming the same anomaly for the same service
        String cooldownKey = COOLDOWN_KEY_PREFIX + event.getServiceName();
        Boolean alreadyFired = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(alreadyFired)) {
            return Optional.empty();
        }

        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        Anomaly anomaly = Anomaly.builder()
                .serviceName(event.getServiceName())
                .anomalyType(Anomaly.AnomalyType.SERVICE_UNAVAILABLE)
                .severity(Anomaly.Severity.CRITICAL)
                .description(String.format(
                        "Service '%s' indicates downstream dependency is unavailable: %s",
                        event.getServiceName(), truncate(event.getMessage(), 200)))
                .detectedAt(Instant.now())
                .traceId(event.getTraceId())
                .logEntryId(event.getId())
                .status(Anomaly.AnomalyStatus.OPEN)
                .rawLogSnapshot(event.getMessage())
                .build();

        log.warn("SERVICE_UNAVAILABLE detected for service={}", event.getServiceName());
        return Optional.of(anomaly);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    @Override
    public String strategyName() {
        return "SERVICE_UNAVAILABLE";
    }
}
