package com.logplatform.incident.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class RcaReportDto {
    private String id;
    private String anomalyId;
    private String serviceName;
    private String rootCauseSummary;
    private String aiAnalysis;
    private List<String> contributingFactors;
    private List<String> recommendations;
    private List<String> affectedServices;
    private String status;
    private String generatedBy;
    private double confidenceScore;
    private Instant createdAt;
}
