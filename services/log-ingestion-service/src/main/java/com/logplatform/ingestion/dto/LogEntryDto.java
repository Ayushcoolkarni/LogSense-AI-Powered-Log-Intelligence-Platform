package com.logplatform.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class LogEntryDto {

    @NotBlank(message = "serviceName is required")
    private String serviceName;

    @NotBlank(message = "logLevel is required")
    @Pattern(regexp = "ERROR|WARN|INFO|DEBUG|TRACE", message = "logLevel must be one of: ERROR, WARN, INFO, DEBUG, TRACE")
    private String logLevel;

    @NotBlank(message = "message is required")
    private String message;

    @NotNull(message = "timestamp is required")
    private Instant timestamp;

    private String traceId;
    private String spanId;
    private String hostName;
    private String environment;
    private Map<String, String> metadata;
    private String stackTrace;
}
