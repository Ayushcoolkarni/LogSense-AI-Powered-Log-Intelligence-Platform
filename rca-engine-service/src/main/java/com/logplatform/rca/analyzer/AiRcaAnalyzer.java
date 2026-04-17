package com.logplatform.rca.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logplatform.rca.dto.AnomalyEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI-powered RCA analyzer. Calls Anthropic Claude API with anomaly context
 * to generate a detailed, context-aware root cause analysis.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AiRcaAnalyzer {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    @Value("${anthropic.api.model:claude-opus-4-20250514}")
    private String model;

    @Value("${anthropic.api.enabled:false}")
    private boolean aiEnabled;

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final int MAX_TOKENS = 1024;

    /**
     * Returns Optional.empty() if AI is disabled or API call fails.
     * Caller falls back to rule-based analysis.
     */
    public Optional<String> generateAnalysis(AnomalyEventDto anomaly) {
        if (!aiEnabled || anthropicApiKey.isBlank()) {
            log.debug("AI analysis disabled or API key not configured");
            return Optional.empty();
        }

        String prompt = buildPrompt(anomaly);

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", MAX_TOKENS,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String responseBody = webClient.post()
                    .uri(ANTHROPIC_API_URL)
                    .header("x-api-key", anthropicApiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            String analysis = root.path("content").get(0).path("text").asText();

            log.info("AI RCA generated for anomaly={} service={}", anomaly.getId(), anomaly.getServiceName());
            return Optional.of(analysis);

        } catch (Exception e) {
            log.error("AI RCA failed for anomaly={}: {}", anomaly.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    private String buildPrompt(AnomalyEventDto anomaly) {
        return String.format("""
                You are an expert Site Reliability Engineer performing Root Cause Analysis.
                Analyze this production anomaly and provide:
                1. A concise root cause hypothesis (2-3 sentences)
                2. Top 3 most likely causes ranked by probability
                3. Immediate remediation steps
                4. Long-term preventive measures
                
                Anomaly Details:
                - Service: %s
                - Type: %s
                - Severity: %s
                - Description: %s
                - Detected At: %s
                - Actual Value: %.1f (Threshold: %.1f)
                - Raw Log Snapshot: %s
                
                Respond in a structured, actionable format. Be specific to the service and anomaly type.
                """,
                anomaly.getServiceName(),
                anomaly.getAnomalyType(),
                anomaly.getSeverity(),
                anomaly.getDescription(),
                anomaly.getDetectedAt(),
                anomaly.getActualValue(),
                anomaly.getThreshold(),
                anomaly.getRawLogSnapshot() != null ? anomaly.getRawLogSnapshot().substring(
                        0, Math.min(300, anomaly.getRawLogSnapshot().length())) : "N/A"
        );
    }
}
