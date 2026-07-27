package com.framl.monitoring.dto;

import com.framl.monitoring.enums.AlertStatus;
import com.framl.monitoring.enums.ResolutionCode;
import lombok.Data;

import java.time.Instant;

@Data
public class AlertHistoryResponse {
    private Long id;
    private Instant changedAt;
    private String actionType;
    private AlertStatus fromStatus;
    private AlertStatus toStatus;
    private String comment;
    private ResolutionCode resolution;
    private String changedBy;
}
