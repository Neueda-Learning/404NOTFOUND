package com.framl.monitoring.dto;

import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.RuleType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RuleRequest {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    private RuleType ruleType;

    @NotNull
    private AlertSeverity severity;

    private Boolean active = true;

    private BigDecimal threshold;
    private Integer maxTransactionCount;
    private Integer timeWindowMinutes;
    private BigDecimal dailyLimit;
    private String currency;
    private List<String> transactionTypes;
}
