package com.framl.monitoring.dto;

import com.framl.monitoring.enums.TransactionStatus;
import com.framl.monitoring.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class TransactionResponse {
    private String transactionId;
    private String accountId;
    private String payeeId;
    private String payeeName;
    private TransactionType type;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private Instant transactionTime;
    private Instant receivedAt;
    private Instant evaluatedAt;
    private String evaluationMode;
    private String paymentChannel;
    private String country;
    private String description;
    private Integer version;
    private Boolean lateArrival;
    private Boolean hasAlert;
    private String alertId;
    private String alertStatus;
}
