package com.logsense.rca.ai;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the AI provider chain:
 *   Gemini (primary) → Ollama (fallback) → RuleBased (backup)
 *
 * Strategy:
 *   1. Iterate providers in declared order.
 *   2. Skip disabled providers (isAvailable() = false).
 *   3. On AiProviderException, log warning and try next.
 *   4. RuleBasedAiProvider is always last and never throws — acts as guaranteed fallback.
 *
 * Metrics emitted (if Micrometer is on classpath):
 *   rca.provider.calls{provider, status}   — counter
 *   rca.provider.duration{provider}        — timer
 */
@Service
public class AiProviderChain {

    private static final Logger log = LoggerFactory.getLogger(AiProviderChain.class);

    /**
     * Provider order matters: first available wins.
     * Spring injects these in declaration order from @Component beans.
     * Explicit ordering here for clarity.
     */
    private final List<AiProvider> providers;
    private final MeterRegistry meterRegistry;

    public AiProviderChain(
            GeminiAiProvider gemini,
            OllamaAiProvider ollama,
            RuleBasedAiProvider ruleBased,
            MeterRegistry meterRegistry) {
        // Explicit order: Gemini → Ollama → RuleBased
        this.providers = List.of(gemini, ollama, ruleBased);
        this.meterRegistry = meterRegistry;
    }

    /**
     * Run RCA through the provider chain.
     *
     * @param prompt fully-built RCA prompt
     * @return RCA analysis text (never null, never empty)
     */
    public AiProviderResult analyze(String prompt) {
        for (AiProvider provider : providers) {
            if (!provider.isAvailable()) {
                log.debug("[Chain] Skipping {} — not available", provider.getName());
                continue;
            }

            long start = System.currentTimeMillis();
            try {
                log.info("[Chain] Trying provider: {}", provider.getName());
                String result = provider.analyze(prompt);
                long durationMs = System.currentTimeMillis() - start;

                recordMetric(provider.getName(), "success", durationMs);
                log.info("[Chain] Provider {} succeeded in {}ms", provider.getName(), durationMs);

                return new AiProviderResult(provider.getName(), result, true);

            } catch (AiProviderException e) {
                long durationMs = System.currentTimeMillis() - start;
                recordMetric(provider.getName(), "failure", durationMs);
                log.warn("[Chain] Provider {} failed ({}ms): {} — trying next",
                        provider.getName(), durationMs, e.getMessage());
            }
        }

        // This should never happen because RuleBasedAiProvider.isAvailable() always returns true
        // and its analyze() never throws. But be defensive:
        log.error("[Chain] All providers exhausted — returning empty analysis");
        return new AiProviderResult("None", "RCA analysis unavailable. All AI providers failed.", false);
    }

    private void recordMetric(String providerName, String status, long durationMs) {
        try {
            meterRegistry.counter("rca.provider.calls",
                    Tags.of("provider", providerName, "status", status)).increment();
            meterRegistry.timer("rca.provider.duration",
                    Tags.of("provider", providerName)).record(
                    java.time.Duration.ofMillis(durationMs));
        } catch (Exception e) {
            // Metrics must never affect core flow
            log.debug("Failed to record metric: {}", e.getMessage());
        }
    }

    /**
     * Result wrapper — carries which provider succeeded and the analysis text.
     */
    public record AiProviderResult(String providerName, String analysis, boolean success) {}
}
