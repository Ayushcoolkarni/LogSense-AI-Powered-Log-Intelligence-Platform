package com.logplatform.incident.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logplatform.incident.dto.AnomalyDto;
import com.logplatform.incident.dto.AlertDto;
import com.logplatform.incident.dto.RcaReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class DownstreamServiceClient {

    private final WebClient webClient;

    @Value("${services.anomaly-detector.url:http://localhost:8082}")
    private String anomalyDetectorUrl;

    @Value("${services.rca-engine.url:http://localhost:8083}")
    private String rcaEngineUrl;

    @Value("${services.alert.url:http://localhost:8084}")
    private String alertServiceUrl;

    // ── Anomaly Detector ────────────────────────────────────────────────────

    public List<AnomalyDto> fetchOpenAnomalies(int page, int size) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(anomalyDetectorUrl + "/api/v1/anomalies?status=OPEN&page={p}&size={s}", page, size)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response != null && response.containsKey("content")) {
                List<?> raw = (List<?>) response.get("content");
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                List<AnomalyDto> anomalies = raw.stream()
                        .map(item -> mapper.convertValue(item, AnomalyDto.class))
                        .toList();
                log.info("Fetched {} open anomalies from anomaly-detector", anomalies.size());
                return anomalies;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch anomalies: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Optional<AnomalyDto> fetchAnomaly(String anomalyId) {
        try {
            AnomalyDto anomaly = webClient.get()
                    .uri(anomalyDetectorUrl + "/api/v1/anomalies/{id}", anomalyId)
                    .retrieve()
                    .bodyToMono(AnomalyDto.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return Optional.ofNullable(anomaly);
        } catch (Exception e) {
            log.warn("Could not fetch anomaly {}: {}", anomalyId, e.getMessage());
            return Optional.empty();
        }
    }

    public Map<String, Object> fetchAnomalyStats() {
        try {
            return webClient.get()
                    .uri(anomalyDetectorUrl + "/api/v1/anomalies/stats")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch anomaly stats: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ── RCA Engine ──────────────────────────────────────────────────────────

    public Optional<RcaReportDto> fetchRcaForAnomaly(String anomalyId) {
        try {
            RcaReportDto rca = webClient.get()
                    .uri(rcaEngineUrl + "/api/v1/rca/anomaly/{id}", anomalyId)
                    .retrieve()
                    .bodyToMono(RcaReportDto.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return Optional.ofNullable(rca);
        } catch (Exception e) {
            log.warn("No RCA found for anomalyId={}: {}", anomalyId, e.getMessage());
            return Optional.empty();
        }
    }

    // ── Alert Service ───────────────────────────────────────────────────────

    public Optional<AlertDto> fetchAlertForAnomaly(String anomalyId) {
        try {
            // Fetch all alerts and find matching anomalyId
            Map<?, ?> page = webClient.get()
                    .uri(alertServiceUrl + "/api/v1/alerts?size=1000")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            // In production, alert-service should expose GET /api/v1/alerts/anomaly/{id}
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Could not fetch alert for anomalyId={}: {}", anomalyId, e.getMessage());
            return Optional.empty();
        }
    }
}
