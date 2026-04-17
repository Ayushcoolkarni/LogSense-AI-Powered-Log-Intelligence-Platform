package com.logplatform.rca.controller;

import com.logplatform.rca.model.RcaReport;
import com.logplatform.rca.repository.RcaReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rca")
@RequiredArgsConstructor
public class RcaController {

    private final RcaReportRepository rcaReportRepository;

    @GetMapping
    public ResponseEntity<Page<RcaReport>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(rcaReportRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RcaReport> getById(@PathVariable String id) {
        return rcaReportRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/anomaly/{anomalyId}")
    public ResponseEntity<RcaReport> getByAnomalyId(@PathVariable String anomalyId) {
        return rcaReportRepository.findByAnomalyId(anomalyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/service/{serviceName}")
    public ResponseEntity<Page<RcaReport>> getByService(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(rcaReportRepository.findByServiceName(serviceName, pageable));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<RcaReport>> getPending() {
        return ResponseEntity.ok(rcaReportRepository.findByStatusOrderByCreatedAtDesc(RcaReport.RcaStatus.IN_PROGRESS));
    }
}
