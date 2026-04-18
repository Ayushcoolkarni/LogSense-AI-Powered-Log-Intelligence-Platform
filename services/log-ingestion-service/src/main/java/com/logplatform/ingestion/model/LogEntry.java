package com.logplatform.ingestion.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "log_entries", indexes = {
        @Index(name = "idx_service_name", columnList = "serviceName"),
        @Index(name = "idx_log_level", columnList = "logLevel"),
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_trace_id", columnList = "traceId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String logLevel; // ERROR, WARN, INFO, DEBUG, TRACE

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private Instant timestamp;

    private String traceId;

    private String spanId;

    private String hostName;

    private String environment; // prod, staging, dev

    @ElementCollection
    @CollectionTable(name = "log_entry_metadata", joinColumns = @JoinColumn(name = "log_entry_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private Map<String, String> metadata;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    @Column(nullable = false)
    private String source; // REST, KAFKA, AGENT, BATCH

    @Column(nullable = false)
    private Instant ingestedAt;

    @PrePersist
    public void prePersist() {
        if (ingestedAt == null) {
            ingestedAt = Instant.now();
        }
    }
}
