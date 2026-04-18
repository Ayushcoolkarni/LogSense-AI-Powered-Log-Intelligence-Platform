package com.logplatform.rca.analyzer;

import com.logplatform.rca.dto.AnomalyEventDto;
import com.logplatform.rca.model.RcaReport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based RCA analyzer. Applies deterministic rules per anomaly type
 * to generate root cause hypotheses and remediation recommendations.
 */
@Component
public class RuleBasedRcaAnalyzer {

    public RcaReport analyze(AnomalyEventDto anomaly) {
        List<String> factors = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        String summary;
        double confidence;

        switch (anomaly.getAnomalyType()) {
            case "ERROR_RATE_SPIKE" -> {
                summary = buildErrorRateSummary(anomaly, factors, recommendations);
                confidence = 0.75;
            }
            case "REPEATED_ERROR_PATTERN" -> {
                summary = buildRepeatedErrorSummary(anomaly, factors, recommendations);
                confidence = 0.80;
            }
            case "SERVICE_UNAVAILABLE" -> {
                summary = buildServiceUnavailableSummary(anomaly, factors, recommendations);
                confidence = 0.85;
            }
            case "LOG_VOLUME_SPIKE" -> {
                summary = buildVolumeSummary(anomaly, factors, recommendations);
                confidence = 0.65;
            }
            case "CASCADING_FAILURE" -> {
                summary = buildCascadingFailureSummary(anomaly, factors, recommendations);
                confidence = 0.70;
            }
            default -> {
                summary = "Anomaly detected in service '" + anomaly.getServiceName()
                        + "'. Rule-based analysis did not match a specific pattern. "
                        + "Manual investigation recommended.";
                factors.add("Unknown anomaly pattern: " + anomaly.getAnomalyType());
                recommendations.add("Manually inspect logs for service: " + anomaly.getServiceName());
                recommendations.add("Check recent deployments and configuration changes");
                confidence = 0.40;
            }
        }

        return RcaReport.builder()
                .anomalyId(anomaly.getId())
                .serviceName(anomaly.getServiceName())
                .rootCauseSummary(summary)
                .contributingFactors(factors)
                .recommendations(recommendations)
                .status(RcaReport.RcaStatus.COMPLETED)
                .generatedBy("RULE_BASED")
                .confidenceScore(confidence)
                .build();
    }

    private String buildErrorRateSummary(AnomalyEventDto a, List<String> factors, List<String> recs) {
        factors.add("Error rate exceeded threshold: " + (int) a.getActualValue()
                + " errors in detection window (threshold: " + (int) a.getThreshold() + ")");
        factors.add("Service: " + a.getServiceName());
        if (a.getTraceId() != null) {
            factors.add("Associated trace ID: " + a.getTraceId());
        }
        factors.add("Anomaly window: " + a.getWindowStart() + " to " + a.getWindowEnd());

        recs.add("Check recent deployments to " + a.getServiceName() + " — error spikes often follow bad deploys");
        recs.add("Inspect stack traces in error logs for common exception types");
        recs.add("Verify downstream dependencies (DB, cache, external APIs) are healthy");
        recs.add("Review circuit breaker state for " + a.getServiceName());
        recs.add("If spike is sustained, consider rolling back the last deployment");

        return String.format(
                "Service '%s' experienced an error rate spike with %d errors detected within the monitoring window. "
                        + "This typically indicates a code regression, dependency failure, or infrastructure issue. "
                        + "The error rate was %.1fx above the configured threshold.",
                a.getServiceName(), (int) a.getActualValue(),
                a.getThreshold() > 0 ? a.getActualValue() / a.getThreshold() : 1.0);
    }

    private String buildRepeatedErrorSummary(AnomalyEventDto a, List<String> factors, List<String> recs) {
        factors.add("Same error pattern repeated " + (int) a.getActualValue() + " times");
        factors.add("Pattern detected in service: " + a.getServiceName());
        if (a.getRawLogSnapshot() != null) {
            factors.add("Error pattern: " + truncate(a.getRawLogSnapshot(), 200));
        }

        recs.add("Identify and fix the root exception causing the repeated failure");
        recs.add("Check for retry loops — the service may be retrying a failing operation without backoff");
        recs.add("Add exponential backoff and jitter to retry logic");
        recs.add("Verify idempotency of retried operations to avoid data corruption");
        recs.add("Consider adding a dead-letter queue for failed messages");

        return String.format(
                "A recurring error pattern was detected in service '%s', repeating %d times within the window. "
                        + "This strongly suggests a retry loop or a persistent failure condition that is not being "
                        + "handled with proper backoff. Investigate the failing operation and add circuit-breaker protection.",
                a.getServiceName(), (int) a.getActualValue());
    }

    private String buildServiceUnavailableSummary(AnomalyEventDto a, List<String> factors, List<String> recs) {
        factors.add("Service reported connection refused or upstream unavailability");
        factors.add("Affected service: " + a.getServiceName());
        if (a.getRawLogSnapshot() != null) {
            factors.add("Error message: " + truncate(a.getRawLogSnapshot(), 200));
        }

        recs.add("Verify the downstream dependency is running and reachable from " + a.getServiceName());
        recs.add("Check Kubernetes pod status and restart counts for the dependency");
        recs.add("Review network policies and firewall rules between services");
        recs.add("Check if Resilience4j circuit breaker is OPEN — may need manual reset");
        recs.add("Inspect health check endpoints of all dependencies");
        recs.add("Scale up the dependency if it is under resource pressure");

        return String.format(
                "Service '%s' is reporting that a downstream dependency is unavailable. "
                        + "This indicates a network connectivity issue, a crashed upstream service, or "
                        + "an open circuit breaker. Immediate investigation of the dependency chain is required.",
                a.getServiceName());
    }

    private String buildVolumeSummary(AnomalyEventDto a, List<String> factors, List<String> recs) {
        factors.add("Log volume exceeded " + (int) a.getActualValue() + " entries in the detection window");
        factors.add("Threshold: " + (int) a.getThreshold() + " entries");
        factors.add("Service: " + a.getServiceName());

        recs.add("Check for infinite loops or runaway retry logic in " + a.getServiceName());
        recs.add("Review recent deployments for accidental verbose logging in production");
        recs.add("Verify log level configuration — DEBUG/TRACE should not be enabled in production");
        recs.add("Check CPU and memory usage — log storms can indicate resource exhaustion");
        recs.add("Consider rate-limiting log emission in the service");

        return String.format(
                "Service '%s' generated %d log entries in a short window, which is %.1fx above normal. "
                        + "This could indicate a log storm caused by an infinite loop, excessive retries, "
                        + "or incorrect log level configuration in the production environment.",
                a.getServiceName(), (int) a.getActualValue(),
                a.getThreshold() > 0 ? a.getActualValue() / a.getThreshold() : 1.0);
    }

    private String buildCascadingFailureSummary(AnomalyEventDto a, List<String> factors, List<String> recs) {
        factors.add("Cascading failure pattern detected originating from: " + a.getServiceName());
        factors.add("Multiple downstream services may be impacted");

        recs.add("Identify the root service that first started failing");
        recs.add("Check circuit breaker states across all services in the dependency chain");
        recs.add("Apply bulkhead pattern to isolate the failing service");
        recs.add("Temporarily increase timeouts to allow partial recovery");
        recs.add("Consider shedding non-critical traffic to protect core service paths");

        return String.format(
                "A cascading failure pattern has been detected starting from service '%s'. "
                        + "Failures in one service are propagating to dependents. "
                        + "Immediate action is required to isolate the failure and prevent platform-wide impact.",
                a.getServiceName());
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
