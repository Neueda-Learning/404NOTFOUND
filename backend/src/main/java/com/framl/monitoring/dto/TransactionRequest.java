package com.framl.monitoring.dto;

import com.framl.monitoring.enums.TransactionStatus;
import com.framl.monitoring.enums.TransactionType;
import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class TransactionRequest {
    @NotBlank
    private String transactionId;

    @NotBlank
    private String accountId;

    private String payeeId;
    private String payeeName;

    @NotNull
    private TransactionType type;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private TransactionStatus status;

    @NotNull
    private Instant transactionTime;

    private String paymentChannel;
    private String country;
    private String description;

}
