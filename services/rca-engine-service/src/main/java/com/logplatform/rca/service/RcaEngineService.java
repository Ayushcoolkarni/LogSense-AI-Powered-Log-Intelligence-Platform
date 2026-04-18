package com.logplatform.rca.service;

import com.logplatform.rca.analyzer.AiRcaAnalyzer;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class RcaEngineService {

    private final RcaReportRepository rcaReportRepository;
    private final RuleBasedRcaAnalyzer ruleBasedAnalyzer;
    private final AiRcaAnalyzer aiRcaAnalyzer;

    @Async
    @Transactional
    public void triggerRca(AnomalyEventDto anomaly) {
        // Deduplicate — only one RCA per anomaly
        Optional<RcaReport> existing = rcaReportRepository.findByAnomalyId(anomaly.getId());
        if (existing.isPresent()) {
            log.debug("RCA already exists for anomalyId={}", anomaly.getId());
            return;
        }

        log.info("Starting RCA for anomalyId={} service={} type={}",
                anomaly.getId(), anomaly.getServiceName(), anomaly.getAnomalyType());

        // Save a PENDING record immediately
        RcaReport pending = RcaReport.builder()
                .anomalyId(anomaly.getId())
                .serviceName(anomaly.getServiceName())
                .rootCauseSummary("Analysis in progress...")
                .status(RcaReport.RcaStatus.IN_PROGRESS)
                .generatedBy("PENDING")
                .confidenceScore(0.0)
                .build();
        RcaReport saved = rcaReportRepository.save(pending);

        try {
            // Step 1: Rule-based analysis (always runs)
            RcaReport ruleResult = ruleBasedAnalyzer.analyze(anomaly);

            // Step 2: AI enrichment (optional, requires API key)
            Optional<String> aiAnalysis = aiRcaAnalyzer.generateAnalysis(anomaly);

            // Merge results
            saved.setRootCauseSummary(ruleResult.getRootCauseSummary());
            saved.setContributingFactors(ruleResult.getContributingFactors());
            saved.setRecommendations(ruleResult.getRecommendations());
            saved.setConfidenceScore(ruleResult.getConfidenceScore());
            saved.setGeneratedBy(aiAnalysis.isPresent() ? "AI+RULE_BASED" : "RULE_BASED");

            aiAnalysis.ifPresent(ai -> {
                saved.setAiAnalysis(ai);
                saved.setConfidenceScore(Math.min(saved.getConfidenceScore() + 0.1, 1.0));
            });

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
}
