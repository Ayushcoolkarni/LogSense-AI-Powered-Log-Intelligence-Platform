package com.logplatform.rca.analyzer;

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
 * AI-powered RCA analyzer using Google Gemini (free tier).
 *
 * Free quota: gemini-2.0-flash → 15 RPM, 1M TPM, 1500 RPD (no credit card)
 * Get key: https://aistudio.google.com/app/apikey
 *
 * Enable via docker-compose.yml environment:
 *   GEMINI_ENABLED=true
 *   GEMINI_API_KEY=AIza...
 *
 * When GEMINI_ENABLED=false (default), falls back to offline rule-based analysis
 * in RcaEngineService — no API calls are made, works fully offline.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AiRcaAnalyzer {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String model;

    @Value("${gemini.api.enabled:false}")
    private boolean aiEnabled;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    /**
     * Returns Optional.empty() when:
     * - GEMINI_ENABLED=false  → fully offline, rule-based RCA used instead
     * - API key missing/blank → same
     * - API call fails        → graceful degradation, rule-based used instead
     *
     * Caller (RcaEngineService) always falls back to RuleBasedRcaAnalyzer.
     */
    public Optional<String> generateAnalysis(AnomalyEventDto anomaly) {
        if (!aiEnabled) {
            log.debug("[Gemini] Disabled (GEMINI_ENABLED=false) — using offline rule-based RCA");
            return Optional.empty();
        }
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("[Gemini] Enabled but GEMINI_API_KEY is not set — falling back to rule-based RCA");
            return Optional.empty();
        }

        String prompt = buildPrompt(anomaly);
        String url    = String.format(GEMINI_URL, model, geminiApiKey);

        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "temperature",     0.3,
                            "maxOutputTokens", 1024,
                            "topP",            0.8
                    )
            );

            String raw = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            var root       = objectMapper.readTree(raw);
            var candidates = root.path("candidates");

            if (candidates.isEmpty()) {
                log.warn("[Gemini] No candidates returned for anomaly={}", anomaly.getId());
                return Optional.empty();
            }

            String analysis = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            if (analysis.isBlank()) {
                log.warn("[Gemini] Empty response for anomaly={}", anomaly.getId());
                return Optional.empty();
            }

            log.info("[Gemini] RCA generated for anomaly={} service={} chars={}",
                    anomaly.getId(), anomaly.getServiceName(), analysis.length());
            return Optional.of(analysis);

        } catch (Exception e) {
            log.error("[Gemini] RCA failed for anomaly={} — falling back to rule-based: {}",
                    anomaly.getId(), e.getMessage());
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
                - Service:      %s
                - Type:         %s
                - Severity:     %s
                - Description:  %s
                - Detected At:  %s
                - Actual Value: %.1f (Threshold: %.1f)
                - Log Snapshot: %s

                Respond in a structured, actionable format. Be specific to the service and anomaly type.
                """,
                anomaly.getServiceName(),
                anomaly.getAnomalyType(),
                anomaly.getSeverity(),
                anomaly.getDescription(),
                anomaly.getDetectedAt(),
                anomaly.getActualValue(),
                anomaly.getThreshold(),
                anomaly.getRawLogSnapshot() != null
                        ? anomaly.getRawLogSnapshot().substring(
                                0, Math.min(300, anomaly.getRawLogSnapshot().length()))
                        : "N/A"
        );
    }
}
