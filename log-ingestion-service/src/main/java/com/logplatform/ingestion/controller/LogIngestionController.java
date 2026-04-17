package com.logplatform.ingestion.controller;

import com.logplatform.ingestion.dto.ApiResponse;
import com.logplatform.ingestion.dto.BatchLogRequest;
import com.logplatform.ingestion.dto.IngestionStatsDto;
import com.logplatform.ingestion.dto.LogEntryDto;
import com.logplatform.ingestion.model.LogEntry;
import com.logplatform.ingestion.service.LogIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Slf4j
public class LogIngestionController {

    private final LogIngestionService logIngestionService;

    /**
     * Ingest a single log entry
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LogEntry>> ingestLog(@Valid @RequestBody LogEntryDto dto) {
        LogEntry saved = logIngestionService.ingestLog(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Log ingested successfully", saved));
    }

    /**
     * Batch ingest up to 1000 log entries
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Integer>> ingestBatch(@Valid @RequestBody BatchLogRequest request) {
        List<LogEntry> saved = logIngestionService.ingestBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Batch ingested successfully", saved.size()));
    }

    /**
     * Query logs with filters and pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<LogEntry>>> queryLogs(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String logLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        PageRequest pageable = PageRequest.of(page, size, sort);
        Page<LogEntry> result = logIngestionService.queryLogs(serviceName, logLevel, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Get all logs for a distributed trace
     */
    @GetMapping("/trace/{traceId}")
    public ResponseEntity<ApiResponse<List<LogEntry>>> getByTraceId(@PathVariable String traceId) {
        List<LogEntry> entries = logIngestionService.queryByTraceId(traceId);
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }

    /**
     * Query logs in a time range
     */
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<Page<LogEntry>>> getByTimeRange(
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEntry> result = logIngestionService.queryByTimeRange(from, to, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Ingestion stats for monitoring dashboard
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<IngestionStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(logIngestionService.getIngestionStats()));
    }
}
