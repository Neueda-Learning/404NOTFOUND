package com.framl.monitoring.dto;

import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.RuleType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class RuleResponse {
    private Long id;
    private String name;
    private String description;
    private RuleType ruleType;
    private AlertSeverity severity;
    private Boolean active;
    private BigDecimal threshold;
    private Integer maxTransactionCount;
    private Integer timeWindowMinutes;
    private BigDecimal dailyLimit;
    private String currency;
    private List<String> transactionTypes;
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;
    private Long alertCount;
    private Long triggerCount;
}
