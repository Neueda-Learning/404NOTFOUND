package com.framl.monitoring.controller;

import com.framl.monitoring.dto.*;
import com.framl.monitoring.enums.TransactionStatus;
import com.framl.monitoring.enums.TransactionType;
import com.framl.monitoring.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> ingest(@Valid @RequestBody TransactionRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.ingest(req));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().startsWith("Duplicate")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(transactionService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<PageResponse<TransactionResponse>> search(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String payeeId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Instant fromTime,
            @RequestParam(required = false) Instant toTime,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.search(accountId, payeeId, status, type, fromTime, toTime, q, page, size));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TransactionResponse> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            String newStatusStr = (String) body.get("status");
            Integer version = body.get("expectedVersion") != null ? (Integer) body.get("expectedVersion") : null;
            TransactionStatus newStatus = TransactionStatus.valueOf(newStatusStr);
            return ResponseEntity.ok(transactionService.updateStatus(id, newStatus, version));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
