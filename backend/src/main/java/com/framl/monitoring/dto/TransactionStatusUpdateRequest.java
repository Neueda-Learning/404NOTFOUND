package com.framl.monitoring.dto;

import com.framl.monitoring.enums.TransactionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TransactionStatusUpdateRequest {

    @NotNull
    private TransactionStatus status;

    @Positive
    private Integer expectedVersion;
}