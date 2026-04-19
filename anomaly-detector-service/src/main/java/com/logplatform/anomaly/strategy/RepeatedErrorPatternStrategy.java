package com.logplatform.anomaly.strategy;

import com.logplatform.anomaly.dto.LogEventDto;
import com.logplatform.anomaly.model.Anomaly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Detects the same error message repeating in a short window.
 * Uses a hash of the message as the Redis key.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RepeatedErrorPatternStrategy implements AnomalyDetectionStrategy {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${anomaly.repeated-error.window-seconds:120}")
    private int windowSeconds;

    @Value("${anomaly.repeated-error.threshold:10}")
    private int repeatThreshold;

    private static final String KEY_PREFIX = "repeated_err:";

    @Override
    public Optional<Anomaly> analyze(LogEventDto event) {
        if (!"ERROR".equals(event.getLogLevel()) && !"WARN".equals(event.getLogLevel())) {
            return Optional.empty();
        }

        // Create a normalized fingerprint (first 100 chars to handle stack trace variations)
        String fingerprint = normalizeMessage(event.getMessage());
        String key = KEY_PREFIX + event.getServiceName() + ":" + fingerprint.hashCode();

        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);

        if (count != null && count == repeatThreshold) {
            Anomaly anomaly = Anomaly.builder()
                    .serviceName(event.getServiceName())
                    .anomalyType(Anomaly.AnomalyType.REPEATED_ERROR_PATTERN)
                    .severity(Anomaly.Severity.MEDIUM)
                    .description(String.format(
                            "Message pattern repeated %d times in %ds for service '%s': %s",
                            count, windowSeconds, event.getServiceName(), fingerprint))
                    .detectedAt(Instant.now())
                    .windowStart(Instant.now().minus(windowSeconds, ChronoUnit.SECONDS))
                    .windowEnd(Instant.now())
                    .actualValue(count)
                    .threshold(repeatThreshold)
                    .traceId(event.getTraceId())
                    .logEntryId(event.getId())
                    .status(Anomaly.AnomalyStatus.OPEN)
                    .rawLogSnapshot(event.getMessage())
                    .build();

            log.warn("REPEATED_ERROR_PATTERN detected for service={} pattern={}", event.getServiceName(), fingerprint);
            return Optional.of(anomaly);
        }

        return Optional.empty();
    }

    private String normalizeMessage(String message) {
        if (message == null) return "";
        // Strip numbers, UUIDs, IPs to normalize dynamic parts
        return message
                .replaceAll("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "<UUID>")
                .replaceAll("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", "<IP>")
                .replaceAll("\\d+", "<N>")
                .substring(0, Math.min(100, message.length()));
    }

    @Override
    public String strategyName() {
        return "REPEATED_ERROR_PATTERN";
    }
}
