package com.logplatform.incident.controller;

import com.logplatform.incident.dto.DashboardStatsDto;
import com.logplatform.incident.model.IncidentSummary;
import com.logplatform.incident.repository.IncidentSummaryRepository;
import com.logplatform.incident.service.IncidentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // React dashboard
public class IncidentController {

    private final IncidentQueryService queryService;
    private final IncidentSummaryRepository repository;

    /**
     * Main dashboard stats endpoint — powers all dashboard KPI cards + charts
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> getDashboard() {
        return ResponseEntity.ok(queryService.getDashboardStats());
    }

    /**
     * Search / filter incidents — powers the incident table
     */
    @GetMapping
    public ResponseEntity<Page<IncidentSummary>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(queryService.search(q, status, serviceName, severity, from, to, page, size));
    }

    /**
     * Full incident detail — includes RCA + alert info
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncidentSummary> getById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get incident by anomaly ID
     */
    @GetMapping("/anomaly/{anomalyId}")
    public ResponseEntity<IncidentSummary> getByAnomalyId(@PathVariable String anomalyId) {
        return repository.findByAnomalyId(anomalyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update incident status (resolve, mark false positive, etc.)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<IncidentSummary> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {

        return repository.findById(id).map(incident -> {
            incident.setStatus(status.toUpperCase());
            return ResponseEntity.ok(repository.save(incident));
        }).orElse(ResponseEntity.notFound().build());
    }
}
