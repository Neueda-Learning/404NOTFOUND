package com.framl.monitoring.dto;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class ApiErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> fieldErrors;
}