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
 * Detects when a service logs at an unusually high volume (all levels).
 * Useful for catching infinite loop bugs or log-bomb issues.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LogVolumeSpikeStrategy implements AnomalyDetectionStrategy {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${anomaly.volume-spike.window-seconds:30}")
    private int windowSeconds;

    @Value("${anomaly.volume-spike.threshold:500}")
    private int volumeThreshold;

    private static final String KEY_PREFIX = "log_volume:";

    @Override
    public Optional<Anomaly> analyze(LogEventDto event) {
        String key = KEY_PREFIX + event.getServiceName();
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);

        if (count != null && count == volumeThreshold) {
            Anomaly anomaly = Anomaly.builder()
                    .serviceName(event.getServiceName())
                    .anomalyType(Anomaly.AnomalyType.LOG_VOLUME_SPIKE)
                    .severity(Anomaly.Severity.MEDIUM)
                    .description(String.format(
                            "Service '%s' produced %d log entries in %d seconds - possible log storm",
                            event.getServiceName(), count, windowSeconds))
                    .detectedAt(Instant.now())
                    .windowStart(Instant.now().minus(windowSeconds, ChronoUnit.SECONDS))
                    .windowEnd(Instant.now())
                    .actualValue(count)
                    .threshold(volumeThreshold)
                    .status(Anomaly.AnomalyStatus.OPEN)
                    .build();

            log.warn("LOG_VOLUME_SPIKE for service={} count={}", event.getServiceName(), count);
            return Optional.of(anomaly);
        }

        return Optional.empty();
    }

    @Override
    public String strategyName() {
        return "LOG_VOLUME_SPIKE";
    }
}
