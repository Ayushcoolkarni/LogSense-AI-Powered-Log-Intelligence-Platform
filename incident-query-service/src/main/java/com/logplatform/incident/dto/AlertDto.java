package com.logplatform.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {
    private String id;
    private String anomalyId;
    private String serviceName;
    private String anomalyType;
    private String severity;
    private String message;
    private String status;
    private List<String> channelsSent;
    private String acknowledgedBy;
    private Instant acknowledgedAt;
    private Instant createdAt;
    private Instant sentAt;
}
