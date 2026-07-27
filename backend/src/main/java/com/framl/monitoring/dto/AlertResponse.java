package com.framl.monitoring.dto;

import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.AlertStatus;
import com.framl.monitoring.enums.ResolutionCode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class AlertResponse {
    private String alertId;
    private String title;
    private String description;
    private String accountId;
    private AlertStatus status;
    private AlertSeverity severity;
    private Integer riskScore;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant acknowledgedAt;
    private Instant investigatingAt;
    private Instant closedAt;
    private Instant dismissedAt;
    private String primaryTransactionId;
    private String primaryPayeeId;
    private BigDecimal totalAmount;
    private String currency;
    private Integer transactionCount;
    private Instant firstTransactionAt;
    private Instant lastTransactionAt;
    private ResolutionCode resolutionCode;
    private String resolution;
    private String resolutionNotes;
    private String comment;
    private Integer version;
    private String ruleId;
    private String ruleName;
    private String ruleType;
    private String triggerReason;
    private BigDecimal actualValue;
    private BigDecimal thresholdValue;
    private Integer timeWindowMinutes;
    private List<AlertHistoryResponse> statusHistory;
    private List<TransactionResponse> transactions;
}
