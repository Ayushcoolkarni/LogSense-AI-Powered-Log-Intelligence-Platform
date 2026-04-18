package com.logplatform.anomaly.strategy;

import com.logplatform.anomaly.dto.LogEventDto;
import com.logplatform.anomaly.model.Anomaly;

import java.util.Optional;

/**
 * Strategy interface for pluggable anomaly detection algorithms.
 * Each strategy is responsible for one detection concern.
 */
public interface AnomalyDetectionStrategy {

    /**
     * Analyze an incoming log event and return an anomaly if detected.
     */
    Optional<Anomaly> analyze(LogEventDto event);

    /**
     * Name of this strategy, used for logging and config toggling.
     */
    String strategyName();
}
