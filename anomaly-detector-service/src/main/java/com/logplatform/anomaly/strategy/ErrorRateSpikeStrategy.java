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
 * Detects ERROR rate spikes using a Redis sliding window counter.
 * If errors for a service exceed threshold within the window → anomaly.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ErrorRateSpikeStrategy implements AnomalyDetectionStrategy {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${anomaly.error-rate.window-seconds:60}")
    private int windowSeconds;

    @Value("${anomaly.error-rate.threshold:50}")
    private int errorThreshold;

    private static final String KEY_PREFIX = "error_rate:";

    @Override
    public Optional<Anomaly> analyze(LogEventDto event) {
        if (!"ERROR".equals(event.getLogLevel())) {
            return Optional.empty();
        }

        String key = KEY_PREFIX + event.getServiceName();
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);

        log.debug("Error count for service={} count={} threshold={}", event.getServiceName(), count, errorThreshold);

        if (count != null && count >= errorThreshold) {
            // Reset counter to avoid flooding duplicate anomalies
            redisTemplate.delete(key);

            Anomaly anomaly = Anomaly.builder()
                    .serviceName(event.getServiceName())
                    .anomalyType(Anomaly.AnomalyType.ERROR_RATE_SPIKE)
                    .severity(count >= errorThreshold * 3 ? Anomaly.Severity.CRITICAL : Anomaly.Severity.HIGH)
                    .description(String.format(
                            "Service '%s' logged %d errors within %d seconds (threshold: %d)",
                            event.getServiceName(), count, windowSeconds, errorThreshold))
                    .detectedAt(Instant.now())
                    .windowStart(Instant.now().minus(windowSeconds, ChronoUnit.SECONDS))
                    .windowEnd(Instant.now())
                    .actualValue(count)
                    .expectedValue(errorThreshold * 0.5)
                    .threshold(errorThreshold)
                    .traceId(event.getTraceId())
                    .logEntryId(event.getId())
                    .status(Anomaly.AnomalyStatus.OPEN)
                    .rawLogSnapshot(event.getMessage())
                    .build();

            log.warn("ERROR_RATE_SPIKE detected for service={} count={}", event.getServiceName(), count);
            return Optional.of(anomaly);
        }

        return Optional.empty();
    }

    @Override
    public String strategyName() {
        return "ERROR_RATE_SPIKE";
    }
}
