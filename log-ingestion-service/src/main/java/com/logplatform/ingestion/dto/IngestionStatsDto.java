package com.logplatform.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionStatsDto {
    private long totalLogsIngested;
    private Map<String, Long> logsByLevel;
    private Map<String, Long> logsByService;
    private long logsInLastHour;
    private long logsInLastDay;
    private Instant lastIngestedAt;
    private double avgIngestionRatePerMinute;
}
