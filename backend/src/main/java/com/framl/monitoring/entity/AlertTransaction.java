package com.framl.monitoring.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "alert_transactions", indexes = {
    @Index(name = "idx_alert_tx_transaction_id", columnList = "transactionId"),
    @Index(name = "idx_alert_tx_alert_id", columnList = "alert_id"),
    @Index(name = "idx_alert_tx_transaction_alert", columnList = "transactionId, alert_id")
})
public class AlertTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @Column(nullable = false, length = 64)
    private String transactionId;

    private Boolean primaryTrigger = false;
}
