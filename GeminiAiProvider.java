package com.logsense.rca.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * PRIMARY AI provider — Google Gemini free tier.
 *
 * Free quota (as of 2025): gemini-2.0-flash → 15 RPM, 1M TPM, 1500 RPD  (no credit card needed)
 * Get key: https://aistudio.google.com/app/apikey
 *
 * Config (application.yml / env):
 *   ai.gemini.enabled=true
 *   ai.gemini.api-key=AIza...
 *   ai.gemini.model=gemini-2.0-flash          # or gemini-1.5-flash
 *   ai.gemini.temperature=0.3
 *   ai.gemini.max-output-tokens=2048
 */
@Component
public class GeminiAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @Value("${ai.gemini.enabled:false}")
    private boolean enabled;

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${ai.gemini.temperature:0.3}")
    private double temperature;

    @Value("${ai.gemini.max-output-tokens:2048}")
    private int maxOutputTokens;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiAiProvider(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "Gemini";
    }

    @Override
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String analyze(String prompt) throws AiProviderException {
        if (!isAvailable()) {
            throw new AiProviderException(getName(), "provider is disabled or API key is missing");
        }

        String url = String.format(BASE_URL, model, apiKey);

        // Build request body following Gemini REST API schema
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", maxOutputTokens,
                        "topP", 0.8,
                        "topK", 40
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<GeminiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    GeminiResponse.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new AiProviderException(getName(),
                        "Non-200 response: " + response.getStatusCode());
            }

            String result = extractText(response.getBody());
            log.info("[Gemini] RCA completed. model={} chars={}", model, result.length());
            return result;

        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[Gemini] API call failed: {}", e.getMessage());
            throw new AiProviderException(getName(), "REST call failed: " + e.getMessage(), e);
        }
    }

    private String extractText(GeminiResponse body) throws AiProviderException {
        if (body.candidates() == null || body.candidates().isEmpty()) {
            throw new AiProviderException(getName(), "Response has no candidates");
        }
        GeminiResponse.Candidate candidate = body.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            throw new AiProviderException(getName(), "Candidate has no content parts");
        }
        return candidate.content().parts().get(0).text();
    }

    // ---- Response DTOs ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeminiResponse(List<Candidate> candidates) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Candidate(Content content, String finishReason) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Content(List<Part> parts) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Part(String text) {}
    }
}
