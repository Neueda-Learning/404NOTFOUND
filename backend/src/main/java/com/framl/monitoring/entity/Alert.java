package com.framl.monitoring.entity;

import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.AlertStatus;
import com.framl.monitoring.enums.ResolutionCode;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_account", columnList = "accountId"),
        @Index(name = "idx_alert_created", columnList = "createdAt"),
        @Index(name = "idx_alert_severity", columnList = "severity"),
        @Index(name = "idx_alert_status_created", columnList = "status, createdAt"),
        @Index(name = "idx_alert_severity_created", columnList = "severity, createdAt"),
        @Index(name = "idx_alert_account_created", columnList = "accountId, createdAt"),
        @Index(name = "idx_alert_primary_tx", columnList = "primaryTransactionId")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_alert_deduplication_key", columnNames = "deduplicationKey")
})
public class Alert {

    @Id
    @Column(nullable = false, length = 40)
    private String alertId;

    @Column(nullable = false, length = 160)
    private String deduplicationKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 64)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status = AlertStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertSeverity severity;

    @Column(nullable = false)
    private Integer riskScore = 0;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    private Instant acknowledgedAt;
    private Instant investigatingAt;
    private Instant closedAt;
    private Instant dismissedAt;

    @Column(length = 64)
    private String primaryTransactionId;

    @Column(length = 64)
    private String primaryPayeeId;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(length = 3)
    private String currency;

    private Integer transactionCount = 0;

    private Instant firstTransactionAt;
    private Instant lastTransactionAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ResolutionCode resolutionCode;

    @Column(length = 1000)
    private String resolution;

    @Column(length = 2000)
    private String resolutionNotes;

    @Column(length = 2000)
    private String comment;

    @Column(nullable = false)
    private Integer version = 0;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AlertTransaction> alertTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AlertHistory> statusHistory = new ArrayList<>();

    @Column(length = 64)
    private String ruleId;

    @Column(length = 100)
    private String ruleName;

    @Column(length = 30)
    private String ruleType;

    @Column(length = 500)
    private String triggerReason;

    @Column(precision = 19, scale = 4)
    private BigDecimal actualValue;

    @Column(precision = 19, scale = 4)
    private BigDecimal thresholdValue;

    private Integer timeWindowMinutes;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
