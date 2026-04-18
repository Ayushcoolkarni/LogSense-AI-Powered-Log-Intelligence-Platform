package com.logplatform.rca.repository;

import com.logplatform.rca.model.RcaReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RcaReportRepository extends JpaRepository<RcaReport, String> {
    Optional<RcaReport> findByAnomalyId(String anomalyId);
    Page<RcaReport> findByServiceName(String serviceName, Pageable pageable);
    List<RcaReport> findByStatusOrderByCreatedAtDesc(RcaReport.RcaStatus status);
}
