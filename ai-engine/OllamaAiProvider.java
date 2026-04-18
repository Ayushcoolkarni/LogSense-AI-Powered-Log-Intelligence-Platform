package com.logsense.rca.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * FALLBACK AI provider — Ollama running locally (or in Docker).
 *
 * Setup:
 *   1. Install: https://ollama.com/download
 *   2. Pull a model: ollama pull llama3.2   OR   ollama pull mistral
 *   3. Run: ollama serve   (default port 11434)
 *
 *   OR add to docker-compose.yml:
 *     ollama:
 *       image: ollama/ollama
 *       ports: ["11434:11434"]
 *       volumes: ["ollama_data:/root/.ollama"]
 *       # After first run: docker exec -it logsense-ollama ollama pull llama3.2
 *
 * Config (application.yml / env):
 *   ai.ollama.enabled=true
 *   ai.ollama.base-url=http://localhost:11434      # or http://ollama:11434 in Docker
 *   ai.ollama.model=llama3.2                       # any locally pulled model
 *   ai.ollama.temperature=0.3
 */
@Component
public class OllamaAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiProvider.class);

    @Value("${ai.ollama.enabled:false}")
    private boolean enabled;

    @Value("${ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ai.ollama.model:llama3.2}")
    private String model;

    @Value("${ai.ollama.temperature:0.3}")
    private double temperature;

    private final RestTemplate restTemplate;

    public OllamaAiProvider(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(120)) // local inference can be slow
                .build();
    }

    @Override
    public String getName() {
        return "Ollama";
    }

    @Override
    public boolean isAvailable() {
        if (!enabled) return false;
        // Lightweight health check against Ollama's /api/tags endpoint
        try {
            ResponseEntity<String> health = restTemplate.getForEntity(
                    baseUrl + "/api/tags", String.class);
            return health.getStatusCode().is2xxSuccessful();
        } catch (ResourceAccessException e) {
            log.debug("[Ollama] Health check failed — Ollama not reachable at {}", baseUrl);
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String analyze(String prompt) throws AiProviderException {
        if (!enabled) {
            throw new AiProviderException(getName(), "provider is disabled");
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                        "temperature", temperature,
                        "num_predict", 2048
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<OllamaResponse> response = restTemplate.exchange(
                    baseUrl + "/api/generate",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    OllamaResponse.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new AiProviderException(getName(),
                        "Non-200 response: " + response.getStatusCode());
            }

            String result = response.getBody().response();
            if (result == null || result.isBlank()) {
                throw new AiProviderException(getName(), "Empty response from Ollama");
            }

            log.info("[Ollama] RCA completed. model={} chars={}", model, result.length());
            return result.trim();

        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[Ollama] API call failed: {}", e.getMessage());
            throw new AiProviderException(getName(), "REST call failed: " + e.getMessage(), e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OllamaResponse(
            String response,
            @JsonProperty("done") boolean done,
            @JsonProperty("total_duration") Long totalDuration
    ) {}
}
