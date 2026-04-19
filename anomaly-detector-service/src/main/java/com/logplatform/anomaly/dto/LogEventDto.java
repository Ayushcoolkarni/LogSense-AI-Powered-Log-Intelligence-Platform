package com.logplatform.anomaly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEventDto {
    private String id;
    private String serviceName;
    private String logLevel;
    private String message;
    private Instant timestamp;
    private String traceId;
    private String spanId;
    private String hostName;
    private String environment;
    private Map<String, String> metadata;
    private String stackTrace;
    private String source;
    private Instant ingestedAt;
}
