package com.framl.monitoring.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardSummary {
    private long openAlerts;
    private long underInvestigation;
    private long todaysAlerts;
    private long highRiskAlerts;
    private long todaysTransactions;
    private double alertRate;
    private List<AlertTrendPoint> alertTrend;
    private Map<String, Long> severityDistribution;
    private List<TopRule> topTriggeredRules;

    @Data
    public static class AlertTrendPoint {
        private String date;
        private long high;
        private long medium;
        private long low;
        private long total;
    }

    @Data
    public static class TopRule {
        private String ruleName;
        private long triggerCount;
    }
}
