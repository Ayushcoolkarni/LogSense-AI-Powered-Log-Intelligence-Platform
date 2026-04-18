package com.logplatform.alert.controller;

import com.logplatform.alert.model.Alert;
import com.logplatform.alert.repository.AlertRepository;
import com.logplatform.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRepository alertRepository;
    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<Page<Alert>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (status != null) {
            return ResponseEntity.ok(alertRepository.findByStatus(Alert.AlertStatus.valueOf(status.toUpperCase()), pageable));
        }
        if (serviceName != null) {
            return ResponseEntity.ok(alertRepository.findByServiceName(serviceName, pageable));
        }
        return ResponseEntity.ok(alertRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alert> getById(@PathVariable String id) {
        return alertRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledge(
            @PathVariable String id,
            @RequestParam(defaultValue = "ops-team") String acknowledgedBy) {
        return ResponseEntity.ok(alertService.acknowledge(id, acknowledgedBy));
    }
}
