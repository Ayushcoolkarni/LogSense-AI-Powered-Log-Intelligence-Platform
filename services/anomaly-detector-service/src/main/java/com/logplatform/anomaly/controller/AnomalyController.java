package com.logplatform.anomaly.controller;

import com.logplatform.anomaly.model.Anomaly;
import com.logplatform.anomaly.repository.AnomalyRepository;
import com.logplatform.anomaly.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyRepository anomalyRepository;
    private final AnomalyDetectionService anomalyDetectionService;

    @GetMapping
    public ResponseEntity<Page<Anomaly>> getAnomalies(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("detectedAt").descending());

        if (serviceName != null) {
            return ResponseEntity.ok(anomalyRepository.findByServiceName(serviceName, pageable));
        }
        if (status != null) {
            return ResponseEntity.ok(anomalyRepository.findByStatus(
                    Anomaly.AnomalyStatus.valueOf(status.toUpperCase()), pageable));
        }
        if (severity != null) {
            return ResponseEntity.ok(anomalyRepository.findBySeverity(
                    Anomaly.Severity.valueOf(severity.toUpperCase()), pageable));
        }
        return ResponseEntity.ok(anomalyRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Anomaly> getById(@PathVariable String id) {
        return anomalyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Anomaly> updateStatus(
            @PathVariable String id,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "system") String resolvedBy) {
        Anomaly updated = anomalyDetectionService.updateStatus(
                id, Anomaly.AnomalyStatus.valueOf(status.toUpperCase()), resolvedBy);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(anomalyDetectionService.getDashboardStats());
    }
}
