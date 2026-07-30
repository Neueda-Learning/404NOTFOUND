package com.framl.monitoring.service;

import com.framl.monitoring.dto.DashboardSummary;
import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.AlertStatus;
import com.framl.monitoring.repository.AlertRepository;
import com.framl.monitoring.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AlertRepository alertRepository;
    private final TransactionRepository transactionRepository;

    public DashboardSummary getSummary() {
        DashboardSummary summary = new DashboardSummary();

        // KPI counts
        summary.setOpenAlerts(alertRepository.countByStatus(AlertStatus.OPEN));
        summary.setUnderInvestigation(alertRepository.countByStatus(AlertStatus.INVESTIGATING));
        summary.setHighRiskAlerts(alertRepository.countBySeverity(AlertSeverity.HIGH));

        // Use latest data timestamp as reference instead of Instant.now()
        // so dashboard shows data relative to the most recent event
        Instant latestTimestamp = alertRepository.findMaxCreatedAt();
        Instant todayStart = latestTimestamp.atZone(ZoneOffset.UTC).toLocalDate()
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        summary.setTodaysAlerts(alertRepository.countByCreatedAtBetween(todayStart, latestTimestamp));
        summary.setTodaysTransactions(transactionRepository.countByTimeRange(todayStart, latestTimestamp));

        long txCount = summary.getTodaysTransactions();
        long alertCount = summary.getTodaysAlerts();
        summary.setAlertRate(txCount > 0 ? (double) alertCount / txCount * 100 : 0);

        // 7-day trend ending at latest data day
        Instant sevenDaysAgo = todayStart.minus(6, ChronoUnit.DAYS);
        List<Object[]> trendRaw = alertRepository.getAlertTrendRaw(sevenDaysAgo);
        summary.setAlertTrend(buildTrend(trendRaw));

        // Severity distribution
        Map<String, Long> severityDist = new LinkedHashMap<>();
        severityDist.put("HIGH", alertRepository.countBySeverity(AlertSeverity.HIGH));
        severityDist.put("MEDIUM", alertRepository.countBySeverity(AlertSeverity.MEDIUM));
        severityDist.put("LOW", alertRepository.countBySeverity(AlertSeverity.LOW));
        summary.setSeverityDistribution(severityDist);

        // Top triggered rules
        List<Object[]> topRulesRaw = alertRepository.getTopTriggeredRules(sevenDaysAgo);
        List<DashboardSummary.TopRule> topRules = new ArrayList<>();
        for (Object[] row : topRulesRaw) {
            DashboardSummary.TopRule tr = new DashboardSummary.TopRule();
            tr.setRuleName(row[0] != null ? row[0].toString() : "Unknown");
            tr.setTriggerCount(row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L);
            topRules.add(tr);
        }
        summary.setTopTriggeredRules(topRules);

        return summary;
    }

    private List<DashboardSummary.AlertTrendPoint> buildTrend(List<Object[]> raw) {
        Map<String, DashboardSummary.AlertTrendPoint> map = new LinkedHashMap<>();
        for (Object[] row : raw) {
            String date = row[0] != null ? row[0].toString() : "Unknown";
            long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            String severity = row[2] != null ? row[2].toString() : "LOW";

            DashboardSummary.AlertTrendPoint point = map.computeIfAbsent(date, d -> {
                DashboardSummary.AlertTrendPoint p = new DashboardSummary.AlertTrendPoint();
                p.setDate(d);
                return p;
            });

            switch (severity) {
                case "HIGH" -> point.setHigh(point.getHigh() + count);
                case "MEDIUM" -> point.setMedium(point.getMedium() + count);
                default -> point.setLow(point.getLow() + count);
            }
            point.setTotal(point.getTotal() + count);
        }
        return new ArrayList<>(map.values());
    }
}
