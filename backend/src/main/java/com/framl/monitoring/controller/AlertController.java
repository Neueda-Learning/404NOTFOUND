package com.framl.monitoring.controller;

import com.framl.monitoring.dto.*;
import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.AlertStatus;
import com.framl.monitoring.service.AlertService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Validated
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<PageResponse<AlertResponse>> search(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ResponseEntity.ok(alertService.search(status, severity, accountId, createdFrom, createdTo, q, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getById(@PathVariable @NotBlank String id) {
        return ResponseEntity.ok(alertService.getById(id));
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<AlertResponse> acknowledge(@PathVariable @NotBlank String id,
                                                      @Valid @RequestBody AlertActionRequest req) {
        return ResponseEntity.ok(alertService.acknowledge(id, req));
    }

    @PostMapping("/{id}/investigate")
    public ResponseEntity<AlertResponse> investigate(@PathVariable @NotBlank String id,
                                                      @Valid @RequestBody AlertActionRequest req) {
        return ResponseEntity.ok(alertService.investigate(id, req));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<AlertResponse> close(@PathVariable @NotBlank String id,
                                                @Valid @RequestBody AlertActionRequest req) {
        return ResponseEntity.ok(alertService.close(id, req));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<AlertResponse> dismiss(@PathVariable @NotBlank String id,
                                                  @Valid @RequestBody AlertActionRequest req) {
        return ResponseEntity.ok(alertService.dismiss(id, req));
    }
}
