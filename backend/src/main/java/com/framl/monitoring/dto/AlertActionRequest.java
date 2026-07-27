package com.framl.monitoring.dto;

import com.framl.monitoring.enums.AlertStatus;
import com.framl.monitoring.enums.ResolutionCode;
import lombok.Data;

@Data
public class AlertActionRequest {
    private String comment;
    private ResolutionCode resolutionCode;
    private String resolution;
    private String resolutionNotes;
    private AlertStatus expectedStatus;
    private Integer expectedVersion;
}
