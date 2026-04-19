package com.logplatform.rca.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyEventDto {
    private String id;
    private String serviceName;
    private String anomalyType;
    private String severity;
    private String description;
    private Instant detectedAt;
    private Instant windowStart;
    private Instant windowEnd;
    private double actualValue;
    private double expectedValue;
    private double threshold;
    private String traceId;
    private String logEntryId;
    private String status;
    private String rawLogSnapshot;
}
