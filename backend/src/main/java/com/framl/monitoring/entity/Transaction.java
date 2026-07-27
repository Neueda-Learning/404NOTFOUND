package com.framl.monitoring.entity;

import com.framl.monitoring.enums.TransactionStatus;
import com.framl.monitoring.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_account_time", columnList = "accountId, transactionTime"),
        @Index(name = "idx_tx_payee", columnList = "payeeId"),
        @Index(name = "idx_tx_status", columnList = "status"),
        @Index(name = "idx_tx_time", columnList = "transactionTime")
})
public class Transaction {

    @Id
    @Column(nullable = false, length = 64)
    private String transactionId;

    @Column(nullable = false, length = 64)
    private String accountId;

    @Column(length = 64)
    private String payeeId;

    @Column(length = 128)
    private String payeeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(nullable = false)
    private Instant transactionTime;

    @Column(nullable = false)
    private Instant receivedAt;

    private Instant evaluatedAt;

    @Column(length = 20)
    private String evaluationMode;

    @Column(length = 20)
    private String paymentChannel;

    @Column(length = 10)
    private String country;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer version = 0;

    private Boolean lateArrival = false;
}
