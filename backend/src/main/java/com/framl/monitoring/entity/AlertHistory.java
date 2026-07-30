package com.framl.monitoring.entity;

import com.framl.monitoring.enums.AlertStatus;
import com.framl.monitoring.enums.ResolutionCode;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@Entity
@Table(name = "alert_history")
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @Column(nullable = false)
    private Instant changedAt;

    @Column(nullable = false, length = 30)
    private String actionType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AlertStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus toStatus;

    @Column(length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ResolutionCode resolution;

    @Column(length = 30)
    private String changedBy = "OPERATOR";

    @PrePersist
    public void prePersist() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
