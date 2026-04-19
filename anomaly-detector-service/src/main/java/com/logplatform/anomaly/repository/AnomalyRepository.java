package com.logplatform.anomaly.repository;

import com.logplatform.anomaly.model.Anomaly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, String> {

    Page<Anomaly> findByServiceName(String serviceName, Pageable pageable);

    Page<Anomaly> findByStatus(Anomaly.AnomalyStatus status, Pageable pageable);

    Page<Anomaly> findBySeverity(Anomaly.Severity severity, Pageable pageable);

    List<Anomaly> findByServiceNameAndStatusOrderByDetectedAtDesc(
            String serviceName, Anomaly.AnomalyStatus status);

    long countByStatusAndDetectedAtAfter(Anomaly.AnomalyStatus status, Instant after);

    @Query("SELECT a FROM Anomaly a WHERE a.detectedAt BETWEEN :from AND :to ORDER BY a.detectedAt DESC")
    List<Anomaly> findInTimeRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT a.serviceName, COUNT(a) FROM Anomaly a WHERE a.status = 'OPEN' GROUP BY a.serviceName ORDER BY COUNT(a) DESC")
    List<Object[]> countOpenAnomaliesByService();

    @Query("SELECT a.anomalyType, COUNT(a) FROM Anomaly a GROUP BY a.anomalyType")
    List<Object[]> countByAnomalyType();
}
