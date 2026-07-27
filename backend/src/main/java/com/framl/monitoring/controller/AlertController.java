package com.framl.monitoring.controller;

import com.framl.monitoring.dto.*;
import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.AlertStatus;
import com.framl.monitoring.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(alertService.search(status, severity, accountId, createdFrom, createdTo, q, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(alertService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<AlertResponse> acknowledge(@PathVariable String id,
                                                      @RequestBody AlertActionRequest req) {
        try {
            return ResponseEntity.ok(alertService.acknowledge(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    @PostMapping("/{id}/investigate")
    public ResponseEntity<AlertResponse> investigate(@PathVariable String id,
                                                      @RequestBody AlertActionRequest req) {
        try {
            return ResponseEntity.ok(alertService.investigate(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<AlertResponse> close(@PathVariable String id,
                                                @RequestBody AlertActionRequest req) {
        try {
            return ResponseEntity.ok(alertService.close(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<AlertResponse> dismiss(@PathVariable String id,
                                                  @RequestBody AlertActionRequest req) {
        try {
            return ResponseEntity.ok(alertService.dismiss(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }
}
