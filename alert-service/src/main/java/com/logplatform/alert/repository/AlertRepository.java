package com.logplatform.alert.repository;

import com.logplatform.alert.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {
    Page<Alert> findByServiceName(String serviceName, Pageable pageable);
    Page<Alert> findByStatus(Alert.AlertStatus status, Pageable pageable);
    List<Alert> findByStatusAndRetryCountLessThan(Alert.AlertStatus status, int maxRetries);
    boolean existsByAnomalyIdAndStatusNot(String anomalyId, Alert.AlertStatus status);
    long countByStatusAndCreatedAtAfter(Alert.AlertStatus status, Instant after);
}
