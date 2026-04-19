package com.logplatform.anomaly.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "anomalies", indexes = {
        @Index(name = "idx_anomaly_service", columnList = "serviceName"),
        @Index(name = "idx_anomaly_type", columnList = "anomalyType"),
        @Index(name = "idx_anomaly_detected_at", columnList = "detectedAt"),
        @Index(name = "idx_anomaly_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Anomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AnomalyType anomalyType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Instant detectedAt;

    private Instant windowStart;
    private Instant windowEnd;

    private double actualValue;
    private double expectedValue;
    private double threshold;

    private String traceId;
    private String logEntryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyStatus status;

    private String resolvedBy;
    private Instant resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String rawLogSnapshot;

    public enum AnomalyType {
        ERROR_RATE_SPIKE,
        LOG_VOLUME_SPIKE,
        REPEATED_ERROR_PATTERN,
        LATENCY_ANOMALY,
        SERVICE_UNAVAILABLE,
        CASCADING_FAILURE,
        MEMORY_LEAK_PATTERN,
        UNUSUAL_LOG_PATTERN
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum AnomalyStatus {
        OPEN, ACKNOWLEDGED, RESOLVED, FALSE_POSITIVE
    }
}
