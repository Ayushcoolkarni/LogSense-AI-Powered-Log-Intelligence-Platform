package com.logplatform.ingestion.service;

import com.logplatform.ingestion.dto.BatchLogRequest;
import com.logplatform.ingestion.dto.IngestionStatsDto;
import com.logplatform.ingestion.dto.LogEntryDto;
import com.logplatform.ingestion.kafka.LogEventProducer;
import com.logplatform.ingestion.model.LogEntry;
import com.logplatform.ingestion.repository.LogEntryRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogIngestionService {

    private final LogEntryRepository logEntryRepository;
    private final LogEventProducer logEventProducer;

    @Transactional
    @RateLimiter(name = "logIngestion")
    @CacheEvict(value = "ingestionStats", allEntries = true)
    public LogEntry ingestLog(LogEntryDto dto) {
        LogEntry entry = mapToEntity(dto);
        entry.setSource("REST");
        LogEntry saved = logEntryRepository.save(entry);
        logEventProducer.publishLogEntry(saved);
        log.info("Ingested log id={} service={} level={}", saved.getId(), saved.getServiceName(), saved.getLogLevel());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "ingestionStats", allEntries = true)
    public List<LogEntry> ingestBatch(BatchLogRequest batchRequest) {
        List<LogEntry> entries = batchRequest.getLogs().stream()
                .map(dto -> {
                    LogEntry e = mapToEntity(dto);
                    e.setSource("BATCH");
                    return e;
                })
                .collect(Collectors.toList());

        List<LogEntry> saved = logEntryRepository.saveAll(entries);
        logEventProducer.publishBatch(saved);
        log.info("Batch ingested {} logs", saved.size());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<LogEntry> queryLogs(String serviceName, String logLevel, Pageable pageable) {
        if (serviceName != null && logLevel != null) {
            return logEntryRepository.findByServiceNameAndLogLevel(serviceName, logLevel, pageable);
        } else if (serviceName != null) {
            return logEntryRepository.findByServiceName(serviceName, pageable);
        } else if (logLevel != null) {
            return logEntryRepository.findByLogLevel(logLevel, pageable);
        }
        return logEntryRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<LogEntry> queryByTraceId(String traceId) {
        return logEntryRepository.findByTraceId(traceId);
    }

    @Transactional(readOnly = true)
    public Page<LogEntry> queryByTimeRange(Instant from, Instant to, Pageable pageable) {
        return logEntryRepository.findByTimestampBetween(from, to, pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "ingestionStats", key = "'global'")
    public IngestionStatsDto getIngestionStats() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);

        long total = logEntryRepository.count();
        long lastHour = logEntryRepository.countByTimestampAfter(oneHourAgo);
        long lastDay = logEntryRepository.countByTimestampAfter(oneDayAgo);
        Instant lastIngested = logEntryRepository.findLastIngestedAt();

        Map<String, Long> byLevel = new HashMap<>();
        logEntryRepository.countByLogLevel().forEach(row ->
                byLevel.put((String) row[0], (Long) row[1]));

        Map<String, Long> byService = new HashMap<>();
        logEntryRepository.countByServiceName().forEach(row ->
                byService.put((String) row[0], (Long) row[1]));

        double avgRate = lastHour / 60.0;

        return IngestionStatsDto.builder()
                .totalLogsIngested(total)
                .logsByLevel(byLevel)
                .logsByService(byService)
                .logsInLastHour(lastHour)
                .logsInLastDay(lastDay)
                .lastIngestedAt(lastIngested)
                .avgIngestionRatePerMinute(avgRate)
                .build();
    }

    private LogEntry mapToEntity(LogEntryDto dto) {
        return LogEntry.builder()
                .serviceName(dto.getServiceName())
                .logLevel(dto.getLogLevel())
                .message(dto.getMessage())
                .timestamp(dto.getTimestamp())
                .traceId(dto.getTraceId())
                .spanId(dto.getSpanId())
                .hostName(dto.getHostName())
                .environment(dto.getEnvironment())
                .metadata(dto.getMetadata())
                .stackTrace(dto.getStackTrace())
                .ingestedAt(Instant.now())
                .build();
    }
}
