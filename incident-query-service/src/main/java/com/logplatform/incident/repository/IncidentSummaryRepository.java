package com.logplatform.incident.repository;

import com.logplatform.incident.model.IncidentSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentSummaryRepository extends JpaRepository<IncidentSummary, String> {

    Optional<IncidentSummary> findByAnomalyId(String anomalyId);

    Page<IncidentSummary> findByServiceName(String serviceName, Pageable pageable);

    Page<IncidentSummary> findByStatus(String status, Pageable pageable);

    Page<IncidentSummary> findBySeverity(String severity, Pageable pageable);

    Page<IncidentSummary> findByServiceNameAndStatus(String serviceName, String status, Pageable pageable);

    Page<IncidentSummary> findByDetectedAtBetween(Instant from, Instant to, Pageable pageable);

    // Full-text search on anomaly description + root cause summary
    @Query("""
            SELECT i FROM IncidentSummary i
            WHERE LOWER(i.anomalyDescription) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(i.rootCauseSummary)   LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(i.serviceName)        LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY i.detectedAt DESC
            """)
    Page<IncidentSummary> searchByText(@Param("query") String query, Pageable pageable);

    // Dashboard stats
    long countByStatus(String status);

    long countBySeverityAndDetectedAtAfter(String severity, Instant after);

    @Query("SELECT i.serviceName, COUNT(i) FROM IncidentSummary i WHERE i.status = 'OPEN' GROUP BY i.serviceName ORDER BY COUNT(i) DESC")
    List<Object[]> countOpenByService();

    @Query("SELECT i.anomalyType, COUNT(i) FROM IncidentSummary i GROUP BY i.anomalyType ORDER BY COUNT(i) DESC")
    List<Object[]> countByAnomalyType();

    @Query("SELECT i.severity, COUNT(i) FROM IncidentSummary i WHERE i.detectedAt >= :since GROUP BY i.severity")
    List<Object[]> countBySeveritySince(@Param("since") Instant since);

    @Query("""
            SELECT i FROM IncidentSummary i
            WHERE i.detectedAt >= :since
            ORDER BY
              CASE i.severity WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END,
              i.detectedAt DESC
            """)
    List<IncidentSummary> findRecentOrderedBySeverity(@Param("since") Instant since, Pageable pageable);

    // Timeline data: incidents bucketed by hour for charts
    @Query(value = """
            SELECT DATE_TRUNC('hour', detected_at) AS hour, COUNT(*) AS count
            FROM incident_summaries
            WHERE detected_at >= :since
            GROUP BY DATE_TRUNC('hour', detected_at)
            ORDER BY hour
            """, nativeQuery = true)
    List<Object[]> getHourlyTimeline(@Param("since") Instant since);
}
