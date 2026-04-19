package com.logplatform.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {

    // Incident counts
    private long totalIncidents;
    private long openIncidents;
    private long resolvedIncidents;
    private long criticalIncidents24h;
    private long highIncidents24h;

    // Breakdowns
    private Map<String, Long> incidentsByService;
    private Map<String, Long> incidentsByType;
    private Map<String, Long> incidentsBySeverity;

    // Timeline (hourly buckets for line chart)
    private List<TimelineBucket> hourlyTimeline;

    // Top impacted services (for leaderboard)
    private List<ServiceImpact> topImpactedServices;

    // Recent incidents (for feed)
    private List<IncidentFeedItem> recentIncidents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineBucket {
        private String hour;   // ISO string
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceImpact {
        private String serviceName;
        private long openIncidents;
        private long criticalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentFeedItem {
        private String id;
        private String anomalyId;
        private String serviceName;
        private String anomalyType;
        private String severity;
        private String status;
        private String description;
        private String detectedAt;
        private boolean hasRca;
    }
}
