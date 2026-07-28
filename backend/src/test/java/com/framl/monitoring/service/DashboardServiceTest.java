package com.framl.monitoring.service;

import com.framl.monitoring.dto.DashboardSummary;
import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.AlertStatus;
import com.framl.monitoring.repository.AlertRepository;
import com.framl.monitoring.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(alertRepository, transactionRepository);
    }

    @Test
    void getSummary_buildsKpisTrendAndRules() {
        when(alertRepository.countByStatus(AlertStatus.OPEN)).thenReturn(8L);
        when(alertRepository.countByStatus(AlertStatus.INVESTIGATING)).thenReturn(3L);
        when(alertRepository.countBySeverity(AlertSeverity.HIGH)).thenReturn(2L);
        when(alertRepository.countBySeverity(AlertSeverity.MEDIUM)).thenReturn(5L);
        when(alertRepository.countBySeverity(AlertSeverity.LOW)).thenReturn(7L);
        when(alertRepository.countByCreatedAtBetween(any(), any())).thenReturn(6L);
        when(transactionRepository.countByTimeRange(any(), any())).thenReturn(24L);

        List<Object[]> trendRaw = List.of(
                new Object[]{"2026-07-26", 2L, "HIGH"},
                new Object[]{"2026-07-26", 1L, "LOW"},
                new Object[]{"2026-07-27", 3L, "MEDIUM"}
        );
        when(alertRepository.getAlertTrendRaw(any())).thenReturn(trendRaw);

        List<Object[]> topRulesRaw = List.of(
                new Object[]{"Large Amount", 4L},
                new Object[]{"Velocity", 2L}
        );
        when(alertRepository.getTopTriggeredRules(any())).thenReturn(topRulesRaw);

        DashboardSummary summary = dashboardService.getSummary();

        assertNotNull(summary);
        assertEquals(8L, summary.getOpenAlerts());
        assertEquals(3L, summary.getUnderInvestigation());
        assertEquals(6L, summary.getTodaysAlerts());
        assertEquals(24L, summary.getTodaysTransactions());
        assertEquals(2L, summary.getHighRiskAlerts());
        assertEquals(25.0, summary.getAlertRate(), 0.001);

        assertNotNull(summary.getSeverityDistribution());
        assertEquals(2L, summary.getSeverityDistribution().get("HIGH"));
        assertEquals(5L, summary.getSeverityDistribution().get("MEDIUM"));
        assertEquals(7L, summary.getSeverityDistribution().get("LOW"));

        assertNotNull(summary.getAlertTrend());
        assertEquals(2, summary.getAlertTrend().size());
        assertEquals("2026-07-26", summary.getAlertTrend().get(0).getDate());
        assertEquals(2L, summary.getAlertTrend().get(0).getHigh());
        assertEquals(1L, summary.getAlertTrend().get(0).getLow());
        assertEquals(3L, summary.getAlertTrend().get(0).getTotal());

        assertEquals("2026-07-27", summary.getAlertTrend().get(1).getDate());
        assertEquals(3L, summary.getAlertTrend().get(1).getMedium());
        assertEquals(3L, summary.getAlertTrend().get(1).getTotal());

        assertNotNull(summary.getTopTriggeredRules());
        assertEquals(2, summary.getTopTriggeredRules().size());
        assertEquals("Large Amount", summary.getTopTriggeredRules().get(0).getRuleName());
        assertEquals(4L, summary.getTopTriggeredRules().get(0).getTriggerCount());
    }

    @Test
    void getSummary_alertRateIsZeroWhenNoTransactions() {
        when(alertRepository.countByStatus(AlertStatus.OPEN)).thenReturn(1L);
        when(alertRepository.countByStatus(AlertStatus.INVESTIGATING)).thenReturn(0L);
        when(alertRepository.countBySeverity(AlertSeverity.HIGH)).thenReturn(1L);
        when(alertRepository.countBySeverity(AlertSeverity.MEDIUM)).thenReturn(0L);
        when(alertRepository.countBySeverity(AlertSeverity.LOW)).thenReturn(0L);
        when(alertRepository.countByCreatedAtBetween(any(), any())).thenReturn(1L);
        when(transactionRepository.countByTimeRange(any(), any())).thenReturn(0L);
        when(alertRepository.getAlertTrendRaw(any())).thenReturn(List.of());
        when(alertRepository.getTopTriggeredRules(any())).thenReturn(List.of());

        DashboardSummary summary = dashboardService.getSummary();

        assertNotNull(summary);
        assertEquals(0.0, summary.getAlertRate(), 0.001);
        assertNotNull(summary.getAlertTrend());
        assertEquals(0, summary.getAlertTrend().size());
        assertNotNull(summary.getTopTriggeredRules());
        assertEquals(0, summary.getTopTriggeredRules().size());
    }
}
