package com.logplatform.incident.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Materialized incident view aggregated from anomaly-detector, rca-engine, alert-service.
 * Written via scheduled sync; read by the dashboard and external consumers.
 */
@Entity
@Table(name = "incident_summaries", indexes = {
        @Index(name = "idx_incident_anomaly_id", columnList = "anomalyId", unique = true),
        @Index(name = "idx_incident_service", columnList = "serviceName"),
        @Index(name = "idx_incident_status", columnList = "status"),
        @Index(name = "idx_incident_severity", columnList = "severity"),
        @Index(name = "idx_incident_detected_at", columnList = "detectedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Anomaly source fields
    @Column(nullable = false, unique = true)
    private String anomalyId;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String anomalyType;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String anomalyDescription;

    @Column(nullable = false)
    private Instant detectedAt;

    private String traceId;

    // RCA fields (nullable — may not exist yet)
    private String rcaReportId;

    @Column(columnDefinition = "TEXT")
    private String rootCauseSummary;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;

    private String rcaGeneratedBy;
    private Double rcaConfidenceScore;
    private Instant rcaCreatedAt;

    // Alert fields (nullable)
    private String alertId;
    private String alertStatus;
    private Instant alertSentAt;
    private String alertAcknowledgedBy;

    // Overall incident status
    @Column(nullable = false)
    private String status; // OPEN, INVESTIGATING, RESOLVED, FALSE_POSITIVE

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
