package com.framl.monitoring.repository;

import com.framl.monitoring.entity.Alert;
import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface AlertRepository extends JpaRepository<Alert, String> {

    @Query("SELECT a FROM Alert a WHERE " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "(:accountId IS NULL OR a.accountId = :accountId) AND " +
           "(:fromTime IS NULL OR a.createdAt >= :fromTime) AND " +
           "(:toTime IS NULL OR a.createdAt <= :toTime) AND " +
           "(:q IS NULL OR a.alertId LIKE :q OR a.accountId LIKE :q OR a.title LIKE :q)")
    Page<Alert> searchAlerts(@Param("status") AlertStatus status,
                              @Param("severity") AlertSeverity severity,
                              @Param("accountId") String accountId,
                              @Param("fromTime") Instant fromTime,
                              @Param("toTime") Instant toTime,
                              @Param("q") String q,
                              Pageable pageable);

    long countByStatus(AlertStatus status);

    long countBySeverity(AlertSeverity severity);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.createdAt >= :start AND a.createdAt <= :end")
    long countByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT CAST(a.createdAt AS date) as date, COUNT(a) as count, a.severity as severity " +
           "FROM Alert a WHERE a.createdAt >= :start GROUP BY CAST(a.createdAt AS date), a.severity ORDER BY CAST(a.createdAt AS date)")
    List<Object[]> getAlertTrendRaw(@Param("start") Instant start);

    @Query("SELECT a.ruleName as ruleName, COUNT(a) as count FROM Alert a " +
           "WHERE a.createdAt >= :start GROUP BY a.ruleName ORDER BY COUNT(a) DESC")
    List<Object[]> getTopTriggeredRules(@Param("start") Instant start);

    @Query("SELECT a FROM Alert a WHERE a.primaryTransactionId = :txId OR " +
           "a.alertId IN (SELECT at.alert.alertId FROM AlertTransaction at WHERE at.transactionId = :txId)")
    List<Alert> findByTransactionId(@Param("txId") String txId);

    @Query("SELECT COALESCE(MAX(a.createdAt), CURRENT_TIMESTAMP) FROM Alert a")
    Instant findMaxCreatedAt();
}
