package com.framl.monitoring.dto;

import com.framl.monitoring.enums.AlertStatus;
import com.framl.monitoring.enums.ResolutionCode;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlertActionRequest {
    @Size(max = 2000)
    private String comment;
    private ResolutionCode resolutionCode;

    @Size(max = 1000)
    private String resolution;

    @Size(max = 2000)
    private String resolutionNotes;
    private AlertStatus expectedStatus;

    @Positive
    private Integer expectedVersion;
}
