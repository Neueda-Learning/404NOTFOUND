package com.framl.monitoring.repository;

import com.framl.monitoring.entity.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {
    List<AlertHistory> findByAlertAlertIdOrderByChangedAtAsc(String alertId);
}
