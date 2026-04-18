package com.logplatform.ingestion.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
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

    @JsonSerialize(using = InstantSerializer.class)      // ← inside class, on the field
    @JsonDeserialize(using = InstantDeserializer.class)  // ← inside class, on the field
    private Instant lastIngestedAt;

    private double avgIngestionRatePerMinute;
}