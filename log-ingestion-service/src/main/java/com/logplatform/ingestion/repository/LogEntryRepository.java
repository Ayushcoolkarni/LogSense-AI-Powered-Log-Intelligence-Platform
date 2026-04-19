package com.logplatform.ingestion.repository;

import com.logplatform.ingestion.model.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, String> {

    Page<LogEntry> findByServiceName(String serviceName, Pageable pageable);

    Page<LogEntry> findByLogLevel(String logLevel, Pageable pageable);

    Page<LogEntry> findByServiceNameAndLogLevel(String serviceName, String logLevel, Pageable pageable);

    Page<LogEntry> findByTimestampBetween(Instant from, Instant to, Pageable pageable);

    List<LogEntry> findByTraceId(String traceId);

    long countByLogLevelAndTimestampAfter(String logLevel, Instant after);

    long countByTimestampAfter(Instant after);

    @Query("SELECT l.logLevel, COUNT(l) FROM LogEntry l GROUP BY l.logLevel")
    List<Object[]> countByLogLevel();

    @Query("SELECT l.serviceName, COUNT(l) FROM LogEntry l GROUP BY l.serviceName")
    List<Object[]> countByServiceName();

    @Query("SELECT l FROM LogEntry l WHERE l.serviceName = :service AND l.timestamp BETWEEN :from AND :to ORDER BY l.timestamp DESC")
    List<LogEntry> findByServiceAndTimeRange(
            @Param("service") String service,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("SELECT MAX(l.ingestedAt) FROM LogEntry l")
    Instant findLastIngestedAt();

    @Query(value = "SELECT COUNT(*) FROM log_entries WHERE ingested_at >= :since", nativeQuery = true)
    long countIngestedSince(@Param("since") Instant since);
}
