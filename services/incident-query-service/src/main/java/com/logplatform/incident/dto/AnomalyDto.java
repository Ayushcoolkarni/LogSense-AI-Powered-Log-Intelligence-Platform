package com.logplatform.incident.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

// ── Anomaly (from anomaly-detector-service) ──────────────────────────────────
@Data
public class AnomalyDto {
    private String id;
    private String serviceName;
    private String anomalyType;
    private String severity;
    private String description;
    private Instant detectedAt;
    private Instant windowStart;
    private Instant windowEnd;
    private double actualValue;
    private double threshold;
    private String traceId;
    private String logEntryId;
    private String status;
    private String rawLogSnapshot;
}
