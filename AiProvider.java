package com.logsense.rca.ai;

/**
 * Strategy interface for AI providers.
 * Implementations: GeminiAiProvider → OllamaAiProvider → RuleBasedAiProvider
 */
public interface AiProvider {

    /**
     * @return provider name for logging/metrics
     */
    String getName();

    /**
     * @return true if this provider is configured and reachable
     */
    boolean isAvailable();

    /**
     * Perform root-cause analysis given a prompt.
     *
     * @param prompt full RCA prompt built by RcaPromptBuilder
     * @return RCA analysis text
     * @throws AiProviderException if the call fails (triggers next fallback)
     */
    String analyze(String prompt) throws AiProviderException;
}
