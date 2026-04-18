package com.logplatform.incident.service;

import com.logplatform.incident.dto.DashboardStatsDto;
import com.logplatform.incident.model.IncidentSummary;
import com.logplatform.incident.repository.IncidentSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentQueryService {

    private final IncidentSummaryRepository repository;

    private static final DateTimeFormatter HOUR_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00:00'Z'").withZone(ZoneOffset.UTC);

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardStats", key = "'main'")
    public DashboardStatsDto getDashboardStats() {
        Instant now = Instant.now();
        Instant oneDayAgo = now.minus(24, ChronoUnit.HOURS);

        long total = repository.count();
        long open = repository.countByStatus("OPEN") + repository.countByStatus("INVESTIGATING");
        long resolved = repository.countByStatus("RESOLVED");
        long critical24h = repository.countBySeverityAndDetectedAtAfter("CRITICAL", oneDayAgo);
        long high24h = repository.countBySeverityAndDetectedAtAfter("HIGH", oneDayAgo);

        // By service
        Map<String, Long> byService = new HashMap<>();
        repository.countOpenByService().forEach(row -> byService.put((String) row[0], (Long) row[1]));

        // By type
        Map<String, Long> byType = new HashMap<>();
        repository.countByAnomalyType().forEach(row -> byType.put((String) row[0], (Long) row[1]));

        // By severity (last 24h)
        Map<String, Long> bySeverity = new HashMap<>();
        repository.countBySeveritySince(oneDayAgo).forEach(row -> bySeverity.put((String) row[0], (Long) row[1]));

        // Hourly timeline (last 24h)
        List<DashboardStatsDto.TimelineBucket> timeline = new ArrayList<>();
        repository.getHourlyTimeline(oneDayAgo).forEach(row -> timeline.add(
                DashboardStatsDto.TimelineBucket.builder()
                        .hour(row[0].toString())
                        .count(((Number) row[1]).longValue())
                        .build()
        ));

        // Top impacted services
        List<DashboardStatsDto.ServiceImpact> topServices = byService.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> DashboardStatsDto.ServiceImpact.builder()
                        .serviceName(e.getKey())
                        .openIncidents(e.getValue())
                        .criticalCount(repository.countBySeverityAndDetectedAtAfter("CRITICAL", oneDayAgo))
                        .build())
                .collect(Collectors.toList());

        // Recent incidents feed
        PageRequest recent = PageRequest.of(0, 10);
        List<IncidentSummary> recentList = repository.findRecentOrderedBySeverity(oneDayAgo, recent);
        List<DashboardStatsDto.IncidentFeedItem> feed = recentList.stream()
                .map(i -> DashboardStatsDto.IncidentFeedItem.builder()
                        .id(i.getId())
                        .anomalyId(i.getAnomalyId())
                        .serviceName(i.getServiceName())
                        .anomalyType(i.getAnomalyType())
                        .severity(i.getSeverity())
                        .status(i.getStatus())
                        .description(truncate(i.getAnomalyDescription(), 120))
                        .detectedAt(i.getDetectedAt().toString())
                        .hasRca(i.getRcaReportId() != null)
                        .build())
                .collect(Collectors.toList());

        return DashboardStatsDto.builder()
                .totalIncidents(total)
                .openIncidents(open)
                .resolvedIncidents(resolved)
                .criticalIncidents24h(critical24h)
                .highIncidents24h(high24h)
                .incidentsByService(byService)
                .incidentsByType(byType)
                .incidentsBySeverity(bySeverity)
                .hourlyTimeline(timeline)
                .topImpactedServices(topServices)
                .recentIncidents(feed)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<IncidentSummary> search(String query, String status, String serviceName,
                                        String severity, Instant from, Instant to,
                                        int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("detectedAt").descending());

        if (query != null && !query.isBlank()) {
            return repository.searchByText(query, pageable);
        }
        if (from != null && to != null) {
            return repository.findByDetectedAtBetween(from, to, pageable);
        }
        if (serviceName != null && status != null) {
            return repository.findByServiceNameAndStatus(serviceName, status, pageable);
        }
        if (serviceName != null) {
            return repository.findByServiceName(serviceName, pageable);
        }
        if (status != null) {
            return repository.findByStatus(status, pageable);
        }
        if (severity != null) {
            return repository.findBySeverity(severity, pageable);
        }
        return repository.findAll(pageable);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
