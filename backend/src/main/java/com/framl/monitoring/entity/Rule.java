package com.framl.monitoring.entity;

import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.RuleType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "rules")
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertSeverity severity;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(precision = 19, scale = 4)
    private BigDecimal threshold;

    private Integer maxTransactionCount;

    private Integer timeWindowMinutes;

    @Column(precision = 19, scale = 4)
    private BigDecimal dailyLimit;

    @Column(length = 3)
    private String currency;

    @ElementCollection
    @CollectionTable(name = "rule_transaction_types", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "transaction_type")
    private List<String> transactionTypes;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        version++;
    }
}
