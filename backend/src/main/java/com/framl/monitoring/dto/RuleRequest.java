package com.framl.monitoring.dto;

import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.RuleType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RuleRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    private RuleType ruleType;

    @NotNull
    private AlertSeverity severity;

    private Boolean active = true;

    @Positive
    private BigDecimal threshold;

    @Positive
    private Integer maxTransactionCount;

    @Positive
    private Integer timeWindowMinutes;

    @Positive
    private BigDecimal dailyLimit;

    @Size(min = 3, max = 3)
    private String currency;
    private List<String> transactionTypes;
}
