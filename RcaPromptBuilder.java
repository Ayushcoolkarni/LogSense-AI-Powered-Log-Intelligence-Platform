package com.logsense.rca.service;

import com.logsense.rca.model.AnomalyEvent;
import org.springframework.stereotype.Component;

/**
 * Builds a rich, structured RCA prompt from an AnomalyEvent.
 *
 * Prompt is provider-agnostic — works the same for Gemini, Ollama, and RuleBased.
 * Structured with clear sections so the AI response is consistent and parseable.
 */
@Component
public class RcaPromptBuilder {

    public String build(AnomalyEvent event) {
        return """
                You are an expert Site Reliability Engineer (SRE) performing root cause analysis
                on a production incident. Analyze the following anomaly and provide a structured report.

                ## Anomaly Context
                - Service: %s
                - Environment: %s
                - Anomaly Type: %s
                - Severity: %s
                - Detected At: %s
                - Error Count: %s
                - Anomaly Score: %s

                ## Log Sample
                ```
                %s
                ```

                ## Error Pattern
                %s

                ---

                Provide a structured Root Cause Analysis with exactly these sections:

                ### 1. Executive Summary
                (2-3 sentences describing what happened)

                ### 2. Root Cause
                (The most likely technical root cause with reasoning)

                ### 3. Contributing Factors
                (Secondary factors that worsened the impact)

                ### 4. Impact Assessment
                (What was affected: users, data, SLAs)

                ### 5. Immediate Actions (< 30 min)
                (Numbered list of steps to stop the bleeding)

                ### 6. Short-term Fixes (< 24 hours)
                (Numbered list of stabilization steps)

                ### 7. Long-term Prevention
                (Architectural or process improvements to prevent recurrence)

                ### 8. Confidence Level
                HIGH / MEDIUM / LOW — with brief justification

                Keep your response concise, actionable, and technically precise.
                """.formatted(
                event.getServiceName(),
                event.getEnvironment() != null ? event.getEnvironment() : "production",
                event.getAnomalyType(),
                event.getSeverity(),
                event.getDetectedAt() != null ? event.getDetectedAt().toString() : "unknown",
                event.getErrorCount() != null ? event.getErrorCount() : "unknown",
                event.getAnomalyScore() != null ? String.format("%.2f", event.getAnomalyScore()) : "N/A",
                event.getLogSample() != null ? event.getLogSample() : "(no log sample available)",
                event.getErrorPattern() != null ? event.getErrorPattern() : "(no specific pattern identified)"
        );
    }
}
