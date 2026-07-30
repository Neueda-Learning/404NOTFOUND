package com.framl.monitoring.dto;

import com.framl.monitoring.enums.TransactionStatus;
import com.framl.monitoring.enums.TransactionType;
import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class TransactionRequest {
    @NotBlank
    @Size(max = 64)
    private String transactionId;

    @NotBlank
    @Size(max = 64)
    private String accountId;

    @Size(max = 64)
    private String payeeId;

    @Size(max = 128)
    private String payeeName;

    @NotNull
    private TransactionType type;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    private TransactionStatus status;

    @NotNull
    private Instant transactionTime;

    @Size(max = 20)
    private String paymentChannel;

    @Size(max = 10)
    private String country;

    @Size(max = 500)
    private String description;

}
