package com.logplatform.alert.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alert_anomaly_id", columnList = "anomalyId"),
        @Index(name = "idx_alert_service", columnList = "serviceName"),
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_severity", columnList = "severity")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String anomalyId;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String anomalyType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertSeverity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    @ElementCollection
    @CollectionTable(name = "alert_channels_sent", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "channel")
    private List<String> channelsSent;

    private String acknowledgedBy;
    private Instant acknowledgedAt;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    private int retryCount;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        if (status == null) status = AlertStatus.PENDING;
        if (retryCount == 0) retryCount = 0;
    }

    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum AlertStatus {
        PENDING, SENT, ACKNOWLEDGED, FAILED, SUPPRESSED
    }
}
