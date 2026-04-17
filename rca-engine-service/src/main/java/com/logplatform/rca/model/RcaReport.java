package com.logplatform.rca.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "rca_reports", indexes = {
        @Index(name = "idx_rca_anomaly_id", columnList = "anomalyId"),
        @Index(name = "idx_rca_service", columnList = "serviceName"),
        @Index(name = "idx_rca_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RcaReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String anomalyId;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rootCauseSummary;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;

    @Column(columnDefinition = "TEXT")
    private String timeline;

    @ElementCollection
    @CollectionTable(name = "rca_contributing_factors", joinColumns = @JoinColumn(name = "rca_id"))
    @Column(name = "factor", columnDefinition = "TEXT")
    private List<String> contributingFactors;

    @ElementCollection
    @CollectionTable(name = "rca_recommendations", joinColumns = @JoinColumn(name = "rca_id"))
    @Column(name = "recommendation", columnDefinition = "TEXT")
    private List<String> recommendations;

    @ElementCollection
    @CollectionTable(name = "rca_affected_services", joinColumns = @JoinColumn(name = "rca_id"))
    @Column(name = "service_name")
    private List<String> affectedServices;

    @Enumerated(EnumType.STRING)
    private RcaStatus status;

    private String generatedBy; // "AI" or "RULE_BASED"

    private double confidenceScore;

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

    public enum RcaStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }
}
