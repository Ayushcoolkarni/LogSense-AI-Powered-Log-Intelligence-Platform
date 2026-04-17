package com.logplatform.incident.service;

import com.logplatform.incident.client.DownstreamServiceClient;
import com.logplatform.incident.dto.AnomalyDto;
import com.logplatform.incident.dto.RcaReportDto;
import com.logplatform.incident.model.IncidentSummary;
import com.logplatform.incident.repository.IncidentSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Periodically syncs anomalies from anomaly-detector-service and enriches
 * them with RCA + alert data into a local materialized view.
 *
 * This gives the dashboard a single fast query endpoint instead of fan-out calls.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentSyncService {

    private final IncidentSummaryRepository repository;
    private final DownstreamServiceClient client;

    @Scheduled(fixedDelay = 30_000) // every 30s
    @Transactional
    public void syncIncidents() {
        try {
            List<AnomalyDto> anomalies = client.fetchOpenAnomalies(0, 200);
            int synced = 0;

            for (AnomalyDto anomaly : anomalies) {
                Optional<IncidentSummary> existing = repository.findByAnomalyId(anomaly.getId());

                if (existing.isEmpty()) {
                    // New incident — create it
                    IncidentSummary incident = buildFromAnomaly(anomaly);
                    enrichWithRca(incident, anomaly.getId());
                    repository.save(incident);
                    synced++;
                } else {
                    // Existing — update RCA + alert fields if they've changed
                    IncidentSummary incident = existing.get();
                    boolean updated = false;

                    if (incident.getRcaReportId() == null) {
                        updated = enrichWithRca(incident, anomaly.getId());
                    }

                    // Sync anomaly status changes
                    if (!anomaly.getStatus().equals(mapAnomalyStatus(incident.getStatus()))) {
                        incident.setStatus(deriveStatus(anomaly.getStatus(), incident.getRcaReportId() != null));
                        updated = true;
                    }

                    if (updated) {
                        repository.save(incident);
                        synced++;
                    }
                }
            }

            if (synced > 0) {
                log.info("Incident sync complete — upserted {} incidents", synced);
            }
        } catch (Exception e) {
            log.error("Incident sync failed: {}", e.getMessage());
        }
    }

    private IncidentSummary buildFromAnomaly(AnomalyDto anomaly) {
        return IncidentSummary.builder()
                .anomalyId(anomaly.getId())
                .serviceName(anomaly.getServiceName())
                .anomalyType(anomaly.getAnomalyType())
                .severity(anomaly.getSeverity())
                .anomalyDescription(anomaly.getDescription())
                .detectedAt(anomaly.getDetectedAt())
                .traceId(anomaly.getTraceId())
                .status("OPEN")
                .build();
    }

    private boolean enrichWithRca(IncidentSummary incident, String anomalyId) {
        Optional<RcaReportDto> rca = client.fetchRcaForAnomaly(anomalyId);
        if (rca.isPresent()) {
            RcaReportDto r = rca.get();
            incident.setRcaReportId(r.getId());
            incident.setRootCauseSummary(r.getRootCauseSummary());
            incident.setAiAnalysis(r.getAiAnalysis());
            incident.setRcaGeneratedBy(r.getGeneratedBy());
            incident.setRcaConfidenceScore(r.getConfidenceScore());
            incident.setRcaCreatedAt(r.getCreatedAt());
            incident.setStatus("INVESTIGATING");
            return true;
        }
        return false;
    }

    private String deriveStatus(String anomalyStatus, boolean hasRca) {
        return switch (anomalyStatus) {
            case "RESOLVED" -> "RESOLVED";
            case "FALSE_POSITIVE" -> "FALSE_POSITIVE";
            case "ACKNOWLEDGED" -> hasRca ? "INVESTIGATING" : "OPEN";
            default -> hasRca ? "INVESTIGATING" : "OPEN";
        };
    }

    private String mapAnomalyStatus(String incidentStatus) {
        return switch (incidentStatus) {
            case "RESOLVED" -> "RESOLVED";
            case "FALSE_POSITIVE" -> "FALSE_POSITIVE";
            default -> "OPEN";
        };
    }
}
