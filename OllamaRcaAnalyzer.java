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
 * Offline LLM RCA analyzer using Ollama (runs locally inside Docker).
 *
 * Model: llama3.2 (default) — ~2GB, good quality, fast on CPU
 * Other options: mistral, phi3, llama3.1
 *
 * Enable via docker-compose.yml:
 *   OLLAMA_ENABLED=true
 *   OLLAMA_URL=http://ollama:11434   (auto-set when using docker-compose)
 *   OLLAMA_MODEL=llama3.2
 *
 * On first start Ollama will pull the model (~2GB download).
 * Subsequent starts use the cached model.
 *
 * Priority in RcaEngineService: Ollama → Gemini → Rule-based
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OllamaRcaAnalyzer {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ollama.enabled:false}")
    private boolean ollamaEnabled;

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:llama3.2}")
    private String ollamaModel;

    /**
     * Returns Optional.empty() if Ollama is disabled or call fails.
     * RcaEngineService falls back to Gemini → rule-based.
     */
    public Optional<String> generateAnalysis(AnomalyEventDto anomaly) {
        if (!ollamaEnabled) {
            log.debug("[Ollama] Disabled (OLLAMA_ENABLED=false)");
            return Optional.empty();
        }

        String prompt = buildPrompt(anomaly);
        String url    = ollamaUrl + "/api/generate";

        try {
            // Ollama /api/generate with stream:false returns a single JSON object
            Map<String, Object> body = Map.of(
                    "model",  ollamaModel,
                    "prompt", prompt,
                    "stream", false,
                    "options", Map.of(
                            "temperature", 0.3,
                            "num_predict", 1024
                    )
            );

            String raw = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(120))   // local LLM can be slow on CPU
                    .block();

            // Ollama response: { "response": "...", "done": true, ... }
            var root     = objectMapper.readTree(raw);
            String reply = root.path("response").asText();

            if (reply.isBlank()) {
                log.warn("[Ollama] Empty response for anomaly={}", anomaly.getId());
                return Optional.empty();
            }

            log.info("[Ollama] RCA generated for anomaly={} service={} model={} chars={}",
                    anomaly.getId(), anomaly.getServiceName(), ollamaModel, reply.length());
            return Optional.of(reply);

        } catch (Exception e) {
            log.error("[Ollama] RCA failed for anomaly={} — falling back: {}",
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
