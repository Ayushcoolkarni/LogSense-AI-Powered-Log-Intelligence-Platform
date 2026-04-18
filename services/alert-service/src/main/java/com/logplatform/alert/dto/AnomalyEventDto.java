package com.logplatform.alert.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class AnomalyEventDto {
    private String id;
    private String serviceName;
    private String anomalyType;
    private String severity;
    private String description;
    private Instant detectedAt;
    private double actualValue;
    private double threshold;
    private String traceId;
    private String rawLogSnapshot;
}
