package com.logsense.rca.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BACKUP AI provider — deterministic rule-based RCA.
 *
 * Always available, zero external dependencies.
 * Extracts key facts from the prompt and builds a structured analysis.
 *
 * This fires only when Gemini AND Ollama both fail.
 */
@Component
public class RuleBasedAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedAiProvider.class);

    // Patterns to extract context from prompt text
    private static final Pattern SERVICE_PATTERN   = Pattern.compile("service[:\\s]+([\\w-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANOMALY_PATTERN   = Pattern.compile("anomaly type[:\\s]+([\\w_]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEVERITY_PATTERN  = Pattern.compile("severity[:\\s]+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ERROR_PATTERN     = Pattern.compile("(?:error|exception)[:\\s]+([^\\n]{1,120})", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNT_PATTERN     = Pattern.compile("(\\d+)\\s+errors?", Pattern.CASE_INSENSITIVE);

    @Override
    public String getName() {
        return "RuleBased";
    }

    @Override
    public boolean isAvailable() {
        return true; // always available — never throws
    }

    @Override
    public String analyze(String prompt) {
        log.info("[RuleBased] Generating deterministic RCA from prompt ({} chars)", prompt.length());

        String service   = extract(SERVICE_PATTERN,  prompt, "unknown-service");
        String anomaly   = extract(ANOMALY_PATTERN,  prompt, "UNKNOWN");
        String severity  = extract(SEVERITY_PATTERN, prompt, "MEDIUM");
        String errorMsg  = extract(ERROR_PATTERN,    prompt, "see logs for details");
        String count     = extract(COUNT_PATTERN,    prompt, "multiple");

        String rootCause   = deriveRootCause(anomaly, errorMsg);
        String remediation = deriveRemediation(anomaly, errorMsg);

        return buildReport(service, anomaly, severity, errorMsg, count, rootCause, remediation);
    }

    // ---- Private helpers ----

    private String extract(Pattern p, String text, String defaultVal) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : defaultVal;
    }

    private String deriveRootCause(String anomaly, String error) {
        String lower = (anomaly + " " + error).toLowerCase();

        if (lower.contains("error_rate") || lower.contains("error rate")) {
            return "Elevated error rate detected. Likely causes: downstream dependency failure, "
                    + "database connection exhaustion, or recent deployment introducing regressions. "
                    + "The spike pattern suggests a sudden infrastructure event rather than gradual load increase.";
        }
        if (lower.contains("connection") || lower.contains("pool")) {
            return "Connection pool exhaustion detected. Root cause is likely insufficient pool sizing "
                    + "for current traffic, long-running transactions holding connections, or a downstream "
                    + "database that has become slow/unavailable, causing connection timeouts to accumulate.";
        }
        if (lower.contains("volume") || lower.contains("spike")) {
            return "Log volume spike detected. This typically indicates a traffic burst, "
                    + "a chatty code path that entered a hot loop, or an upstream service retrying "
                    + "aggressively against a failing endpoint.";
        }
        if (lower.contains("unavailable") || lower.contains("503") || lower.contains("circuit")) {
            return "Service unavailability detected. Root cause is likely a failed health check causing "
                    + "the load balancer to route traffic to degraded instances, or a circuit breaker "
                    + "that has opened due to repeated downstream failures.";
        }
        if (lower.contains("repeated") || lower.contains("pattern")) {
            return "Repeated error pattern detected. The same error fingerprint is occurring at high "
                    + "frequency, pointing to a specific code path that is consistently failing — "
                    + "likely an unhandled edge case or a persistent infrastructure issue.";
        }
        return "Anomaly detected in service logs. Pattern analysis suggests an infrastructure or "
                + "application-level fault. Manual investigation of recent deployments, dependency "
                + "health, and resource utilisation is recommended.";
    }

    private String deriveRemediation(String anomaly, String error) {
        String lower = (anomaly + " " + error).toLowerCase();

        if (lower.contains("connection") || lower.contains("pool")) {
            return """
                    Immediate:
                      1. Check DB connection pool metrics (active/idle/waiting).
                      2. Kill long-running queries holding connections.
                      3. Restart affected service pod/instance to flush hung connections.
                    Short-term:
                      4. Increase pool size (spring.datasource.hikari.maximum-pool-size).
                      5. Set connection timeout: connectionTimeout=3000ms, idleTimeout=600000ms.
                    Long-term:
                      6. Add a connection pool dashboard alert at 80% utilisation.
                      7. Enable Hikari metrics via Micrometer for proactive monitoring.""";
        }
        if (lower.contains("error_rate") || lower.contains("error rate")) {
            return """
                    Immediate:
                      1. Check downstream dependency health (DB, external APIs, message broker).
                      2. Review recent deployments — consider rolling back if correlated.
                      3. Check Resilience4j circuit breaker state for this service.
                    Short-term:
                      4. Increase retry backoff to reduce thundering herd on recovery.
                      5. Enable bulkhead pattern to isolate failing call paths.
                    Long-term:
                      6. Add error-rate SLO alert at 1% sustained for > 2 minutes.""";
        }
        if (lower.contains("unavailable") || lower.contains("circuit")) {
            return """
                    Immediate:
                      1. Verify service health via /actuator/health.
                      2. Check Kubernetes pod status (kubectl get pods -n <namespace>).
                      3. Reset circuit breaker if downstream has recovered.
                    Short-term:
                      4. Ensure liveness/readiness probes are correctly tuned.
                      5. Review resource limits — OOMKill can cause apparent unavailability.
                    Long-term:
                      6. Implement graceful degradation / fallback responses.""";
        }
        return """
                Immediate:
                  1. Review application logs around the anomaly timestamp.
                  2. Check infrastructure health (CPU, memory, disk, network).
                  3. Verify all downstream dependencies are reachable.
                Short-term:
                  4. Correlate with recent deployments or config changes.
                  5. Increase log verbosity temporarily to capture root cause.
                Long-term:
                  6. Add anomaly-specific alerting thresholds and runbooks.""";
    }

    private String buildReport(String service, String anomaly, String severity,
                               String errorMsg, String count, String rootCause, String remediation) {
        return """
                ## Root Cause Analysis Report
                **Generated by:** Rule-Based Engine (AI providers unavailable)
                **Timestamp:** %s

                ---

                ### Incident Summary
                | Field        | Value             |
                |-------------|-------------------|
                | Service      | `%s`             |
                | Anomaly Type | `%s`             |
                | Severity     | `%s`             |
                | Error Count  | %s               |

                ---

                ### Observed Error
                ```
                %s
                ```

                ---

                ### Root Cause Analysis
                %s

                ---

                ### Recommended Remediation
                %s

                ---

                ### Confidence
                **LOW** — This analysis was generated by deterministic rules because AI providers
                (Gemini and Ollama) were unavailable at the time of analysis.
                Please treat this as a starting point for manual investigation.
                """.formatted(
                Instant.now().toString(),
                service, anomaly, severity, count,
                errorMsg, rootCause, remediation
        );
    }
}
