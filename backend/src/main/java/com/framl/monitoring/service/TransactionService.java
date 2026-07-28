package com.framl.monitoring.service;

import com.framl.monitoring.dto.*;
import com.framl.monitoring.entity.*;
import com.framl.monitoring.enums.*;
import com.framl.monitoring.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final RuleEvaluationService ruleEvaluationService;

    @Transactional
    public TransactionResponse ingest(TransactionRequest req) {
        if (transactionRepository.existsById(req.getTransactionId())) {
            throw new IllegalArgumentException("Duplicate transactionId: " + req.getTransactionId());
        }

        Transaction tx = new Transaction();
        tx.setTransactionId(req.getTransactionId());
        tx.setAccountId(req.getAccountId());
        tx.setPayeeId(req.getPayeeId());
        tx.setPayeeName(req.getPayeeName());
        tx.setType(req.getType());
        tx.setAmount(req.getAmount());
        tx.setCurrency(req.getCurrency());
        tx.setStatus(req.getStatus());
        tx.setTransactionTime(req.getTransactionTime());
        tx.setReceivedAt(Instant.now());
        tx.setPaymentChannel(req.getPaymentChannel());
        tx.setCountry(req.getCountry());
        tx.setDescription(req.getDescription());
        tx.setEvaluationMode("REAL_TIME");

        // Check late arrival
        long diffMinutes = ChronoUnit.MINUTES.between(req.getTransactionTime(), tx.getReceivedAt());
        if (diffMinutes > 5) {
            tx.setLateArrival(true);
        }

        Transaction saved = transactionRepository.save(tx);

        // Evaluate rules if COMPLETED
        if (saved.getStatus() == TransactionStatus.COMPLETED) {
            ruleEvaluationService.evaluate(saved);
            saved.setEvaluatedAt(Instant.now());
            saved = transactionRepository.save(saved);
        }

        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse updateStatus(String id, TransactionStatus newStatus, Integer expectedVersion) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));

        if (expectedVersion != null && !tx.getVersion().equals(expectedVersion)) {
            throw new IllegalStateException("Version conflict");
        }

        TransactionStatus old = tx.getStatus();
        // Validate state transitions
        if (old == TransactionStatus.FAILED || old == TransactionStatus.CANCELLED || old == TransactionStatus.REVERSED) {
            throw new IllegalStateException("Cannot update terminal status: " + old);
        }

        tx.setStatus(newStatus);

        if (newStatus == TransactionStatus.COMPLETED && tx.getEvaluatedAt() == null) {
            ruleEvaluationService.evaluate(tx);
            tx.setEvaluatedAt(Instant.now());
        }

        tx.setVersion(tx.getVersion() + 1);
        return toResponse(transactionRepository.save(tx));
    }

    public PageResponse<TransactionResponse> search(String accountId, String payeeId,
                                                     TransactionStatus status, TransactionType type,
                                                     Instant fromTime, Instant toTime, String q,
                                                     int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionTime").descending());
        String likeQ = q != null ? "%" + q + "%" : null;

        Page<Transaction> result = transactionRepository.searchTransactions(
                accountId, payeeId, status, type, fromTime, toTime, likeQ, pageable);

        return buildPage(result.map(this::toResponse));
    }

    public TransactionResponse getById(String id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
        return toResponse(tx);
    }

    public TransactionResponse toResponse(Transaction tx) {
        TransactionResponse r = new TransactionResponse();
        r.setTransactionId(tx.getTransactionId());
        r.setAccountId(tx.getAccountId());
        r.setPayeeId(tx.getPayeeId());
        r.setPayeeName(tx.getPayeeName());
        r.setType(tx.getType());
        r.setAmount(tx.getAmount());
        r.setCurrency(tx.getCurrency());
        r.setStatus(tx.getStatus());
        r.setTransactionTime(tx.getTransactionTime());
        r.setReceivedAt(tx.getReceivedAt());
        r.setEvaluatedAt(tx.getEvaluatedAt());
        r.setEvaluationMode(tx.getEvaluationMode());
        r.setPaymentChannel(tx.getPaymentChannel());
        r.setCountry(tx.getCountry());
        r.setDescription(tx.getDescription());
        r.setVersion(tx.getVersion());
        r.setLateArrival(tx.getLateArrival());

        // Check for alerts
        List<Alert> alerts = alertRepository.findByTransactionId(tx.getTransactionId());
        r.setHasAlert(!alerts.isEmpty());
        r.setAlertCount(alerts.size());
        r.setAlertIds(alerts.stream().map(Alert::getAlertId).collect(Collectors.toList()));
        if (!alerts.isEmpty()) {
            Alert firstAlert = alerts.get(0);
            r.setAlertId(firstAlert.getAlertId());
            r.setAlertStatus(firstAlert.getStatus().name());
        }

        return r;
    }

    private <T> PageResponse<T> buildPage(Page<T> p) {
        PageResponse<T> resp = new PageResponse<>();
        resp.setContent(p.getContent());
        resp.setPage(p.getNumber());
        resp.setSize(p.getSize());
        resp.setTotalElements(p.getTotalElements());
        resp.setTotalPages(p.getTotalPages());
        resp.setLast(p.isLast());
        return resp;
    }
}
