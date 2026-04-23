package com.logplatform.rca.service;

import com.logplatform.rca.analyzer.AiRcaAnalyzer;
import com.logplatform.rca.analyzer.OllamaRcaAnalyzer;
import com.logplatform.rca.analyzer.RuleBasedRcaAnalyzer;
import com.logplatform.rca.dto.AnomalyEventDto;
import com.logplatform.rca.model.RcaReport;
import com.logplatform.rca.repository.RcaReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * RCA Engine — analysis priority:
 *   1. Ollama  (offline local LLM, OLLAMA_ENABLED=true)
 *   2. Gemini  (online API,        GEMINI_ENABLED=true)
 *   3. Rule-based (always runs as final fallback)
 *
 * Rule-based ALWAYS runs first to populate factors + recommendations.
 * LLM enrichment (Ollama or Gemini) adds aiAnalysis on top if available.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RcaEngineService {

    private final RcaReportRepository  rcaReportRepository;
    private final RuleBasedRcaAnalyzer ruleBasedAnalyzer;
    private final OllamaRcaAnalyzer    ollamaAnalyzer;
    private final AiRcaAnalyzer        geminiAnalyzer;

    @Async
    @Transactional
    public void triggerRca(AnomalyEventDto anomaly) {
        // Deduplicate — only one RCA per anomaly
        if (rcaReportRepository.findByAnomalyId(anomaly.getId()).isPresent()) {
            log.debug("RCA already exists for anomalyId={}", anomaly.getId());
            return;
        }

        log.info("Starting RCA for anomalyId={} service={} type={}",
                anomaly.getId(), anomaly.getServiceName(), anomaly.getAnomalyType());

        // Save PENDING record immediately so dashboard shows activity
        RcaReport saved = rcaReportRepository.save(RcaReport.builder()
                .anomalyId(anomaly.getId())
                .serviceName(anomaly.getServiceName())
                .rootCauseSummary("Analysis in progress...")
                .status(RcaReport.RcaStatus.IN_PROGRESS)
                .generatedBy("PENDING")
                .confidenceScore(0.0)
                .build());

        try {
            // ── Step 1: Rule-based (always runs, provides baseline) ──────────
            RcaReport ruleResult = ruleBasedAnalyzer.analyze(anomaly);
            saved.setRootCauseSummary(ruleResult.getRootCauseSummary());
            saved.setContributingFactors(ruleResult.getContributingFactors());
            saved.setRecommendations(ruleResult.getRecommendations());
            saved.setConfidenceScore(ruleResult.getConfidenceScore());
            saved.setGeneratedBy("RULE_BASED");

            // ── Step 2: LLM enrichment (Ollama first, Gemini as fallback) ───
            Optional<String> llmAnalysis = tryOllama(anomaly)
                    .or(() -> tryGemini(anomaly));

            if (llmAnalysis.isPresent()) {
                saved.setAiAnalysis(llmAnalysis.get());
                saved.setConfidenceScore(Math.min(saved.getConfidenceScore() + 0.1, 1.0));

                // Label which LLM actually ran
                boolean usedOllama = ollamaAnalyzer.generateAnalysis(anomaly).isPresent();
                saved.setGeneratedBy(usedOllama ? "OLLAMA+RULE_BASED" : "GEMINI+RULE_BASED");
            }

            saved.setStatus(RcaReport.RcaStatus.COMPLETED);
            rcaReportRepository.save(saved);

            log.info("RCA completed for anomalyId={} reportId={} generatedBy={}",
                    anomaly.getId(), saved.getId(), saved.getGeneratedBy());

        } catch (Exception e) {
            log.error("RCA failed for anomalyId={}: {}", anomaly.getId(), e.getMessage(), e);
            saved.setStatus(RcaReport.RcaStatus.FAILED);
            saved.setRootCauseSummary("RCA generation failed: " + e.getMessage());
            rcaReportRepository.save(saved);
        }
    }

    private Optional<String> tryOllama(AnomalyEventDto anomaly) {
        try {
            return ollamaAnalyzer.generateAnalysis(anomaly);
        } catch (Exception e) {
            log.warn("[Ollama] Unexpected error, skipping: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> tryGemini(AnomalyEventDto anomaly) {
        try {
            return geminiAnalyzer.generateAnalysis(anomaly);
        } catch (Exception e) {
            log.warn("[Gemini] Unexpected error, skipping: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
