package com.logsense.rca.service;

import com.logsense.rca.ai.AiProviderChain;
import com.logsense.rca.ai.AiProviderChain.AiProviderResult;
import com.logsense.rca.model.AnomalyEvent;
import com.logsense.rca.model.RcaReport;
import com.logsense.rca.model.RcaStatus;
import com.logsense.rca.repository.RcaReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Core RCA orchestration service.
 *
 * Flow:
 *   AnomalyEvent (from Kafka) → build prompt → AiProviderChain → persist RcaReport
 *
 * The AiProviderChain handles Gemini → Ollama → RuleBased internally.
 * This service only cares about the final result.
 */
@Service
public class RcaEngineService {

    private static final Logger log = LoggerFactory.getLogger(RcaEngineService.class);

    private final AiProviderChain aiProviderChain;
    private final RcaReportRepository rcaReportRepository;
    private final RcaPromptBuilder promptBuilder;

    public RcaEngineService(AiProviderChain aiProviderChain,
                            RcaReportRepository rcaReportRepository,
                            RcaPromptBuilder promptBuilder) {
        this.aiProviderChain = aiProviderChain;
        this.rcaReportRepository = rcaReportRepository;
        this.promptBuilder = promptBuilder;
    }

    @Transactional
    public RcaReport generateRca(AnomalyEvent event) {
        log.info("[RCA] Starting analysis for anomalyId={} service={}",
                event.getAnomalyId(), event.getServiceName());

        // Idempotency check — one RCA per anomaly
        return rcaReportRepository.findByAnomalyId(event.getAnomalyId())
                .orElseGet(() -> doGenerateRca(event));
    }

    private RcaReport doGenerateRca(AnomalyEvent event) {
        // 1. Create a pending report immediately so status is visible
        RcaReport report = RcaReport.builder()
                .anomalyId(event.getAnomalyId())
                .serviceName(event.getServiceName())
                .anomalyType(event.getAnomalyType())
                .severity(event.getSeverity())
                .status(RcaStatus.PROCESSING)
                .createdAt(Instant.now())
                .build();
        rcaReportRepository.save(report);

        try {
            // 2. Build prompt from event context
            String prompt = promptBuilder.build(event);
            log.debug("[RCA] Prompt built ({} chars) for anomalyId={}", prompt.length(), event.getAnomalyId());

            // 3. Run through provider chain (Gemini → Ollama → RuleBased)
            AiProviderResult result = aiProviderChain.analyze(prompt);

            // 4. Persist final report
            report.setAnalysis(result.analysis());
            report.setProviderUsed(result.providerName());
            report.setStatus(RcaStatus.COMPLETED);
            report.setCompletedAt(Instant.now());

            log.info("[RCA] Completed for anomalyId={} via provider={}",
                    event.getAnomalyId(), result.providerName());

        } catch (Exception e) {
            log.error("[RCA] Unexpected failure for anomalyId={}: {}", event.getAnomalyId(), e.getMessage(), e);
            report.setAnalysis("RCA failed: " + e.getMessage());
            report.setStatus(RcaStatus.FAILED);
            report.setCompletedAt(Instant.now());
        }

        return rcaReportRepository.save(report);
    }
}
