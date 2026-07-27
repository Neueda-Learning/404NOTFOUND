package com.framl.monitoring.service;

import com.framl.monitoring.dto.*;
import com.framl.monitoring.entity.*;
import com.framl.monitoring.enums.*;
import com.framl.monitoring.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    public PageResponse<AlertResponse> search(AlertStatus status, AlertSeverity severity,
                                               String accountId, Instant fromTime, Instant toTime,
                                               String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String likeQ = q != null ? "%" + q + "%" : null;

        Page<Alert> result = alertRepository.searchAlerts(status, severity, accountId, fromTime, toTime, likeQ, pageable);
        return buildPage(result.map(a -> toResponse(a, false)));
    }

    public AlertResponse getById(String id) {
        Alert alert = findAlert(id);
        return toResponse(alert, true);
    }

    @Transactional
    public AlertResponse acknowledge(String id, AlertActionRequest req) {
        Alert alert = findAlert(id);
        validateTransition(alert, AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED, req);

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(Instant.now());
        if (req.getComment() != null) alert.setComment(req.getComment());
        alert.setVersion(alert.getVersion() + 1);
        alert = alertRepository.save(alert);

        recordHistory(alert, "ACKNOWLEDGED", AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED, req.getComment(), null);
        return toResponse(alert, true);
    }

    @Transactional
    public AlertResponse investigate(String id, AlertActionRequest req) {
        Alert alert = findAlert(id);
        validateTransition(alert, AlertStatus.ACKNOWLEDGED, AlertStatus.INVESTIGATING, req);

        alert.setStatus(AlertStatus.INVESTIGATING);
        alert.setInvestigatingAt(Instant.now());
        if (req.getComment() != null) alert.setComment(req.getComment());
        alert.setVersion(alert.getVersion() + 1);
        alert = alertRepository.save(alert);

        recordHistory(alert, "INVESTIGATING", AlertStatus.ACKNOWLEDGED, AlertStatus.INVESTIGATING, req.getComment(), null);
        return toResponse(alert, true);
    }

    @Transactional
    public AlertResponse close(String id, AlertActionRequest req) {
        Alert alert = findAlert(id);
        validateTransition(alert, AlertStatus.INVESTIGATING, AlertStatus.CLOSED, req);

        alert.setStatus(AlertStatus.CLOSED);
        alert.setClosedAt(Instant.now());
        alert.setResolutionCode(req.getResolutionCode());
        alert.setResolution(req.getResolution());
        alert.setResolutionNotes(req.getResolutionNotes());
        if (req.getComment() != null) alert.setComment(req.getComment());
        alert.setVersion(alert.getVersion() + 1);
        alert = alertRepository.save(alert);

        recordHistory(alert, "CLOSED", AlertStatus.INVESTIGATING, AlertStatus.CLOSED, req.getComment(), req.getResolutionCode());
        return toResponse(alert, true);
    }

    @Transactional
    public AlertResponse dismiss(String id, AlertActionRequest req) {
        Alert alert = findAlert(id);
        AlertStatus fromStatus = alert.getStatus();

        if (fromStatus == AlertStatus.CLOSED || fromStatus == AlertStatus.DISMISSED) {
            throw new IllegalStateException("Cannot dismiss alert in status: " + fromStatus);
        }

        alert.setStatus(AlertStatus.DISMISSED);
        alert.setDismissedAt(Instant.now());
        alert.setResolutionCode(req.getResolutionCode());
        alert.setResolution(req.getResolution());
        alert.setResolutionNotes(req.getResolutionNotes());
        if (req.getComment() != null) alert.setComment(req.getComment());
        alert.setVersion(alert.getVersion() + 1);
        alert = alertRepository.save(alert);

        recordHistory(alert, "DISMISSED", fromStatus, AlertStatus.DISMISSED, req.getComment(), req.getResolutionCode());
        return toResponse(alert, true);
    }

    private void validateTransition(Alert alert, AlertStatus expectedFrom, AlertStatus expectedTo, AlertActionRequest req) {
        if (req.getExpectedStatus() != null && alert.getStatus() != req.getExpectedStatus()) {
            throw new IllegalStateException("Status mismatch: expected " + req.getExpectedStatus() + " but was " + alert.getStatus());
        }
        if (req.getExpectedVersion() != null && !alert.getVersion().equals(req.getExpectedVersion())) {
            throw new IllegalStateException("Version conflict");
        }
        if (alert.getStatus() != expectedFrom) {
            throw new IllegalStateException("Cannot transition from " + alert.getStatus() + " to " + expectedTo);
        }
    }

    private void recordHistory(Alert alert, String action, AlertStatus from, AlertStatus to, String comment, ResolutionCode resolution) {
        AlertHistory h = new AlertHistory();
        h.setAlert(alert);
        h.setActionType(action);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setComment(comment);
        h.setResolution(resolution);
        alertHistoryRepository.save(h);
    }

    private Alert findAlert(String id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
    }

    public AlertResponse toResponse(Alert alert, boolean includeDetails) {
        AlertResponse r = new AlertResponse();
        r.setAlertId(alert.getAlertId());
        r.setTitle(alert.getTitle());
        r.setDescription(alert.getDescription());
        r.setAccountId(alert.getAccountId());
        r.setStatus(alert.getStatus());
        r.setSeverity(alert.getSeverity());
        r.setRiskScore(alert.getRiskScore());
        r.setCreatedAt(alert.getCreatedAt());
        r.setUpdatedAt(alert.getUpdatedAt());
        r.setAcknowledgedAt(alert.getAcknowledgedAt());
        r.setInvestigatingAt(alert.getInvestigatingAt());
        r.setClosedAt(alert.getClosedAt());
        r.setDismissedAt(alert.getDismissedAt());
        r.setPrimaryTransactionId(alert.getPrimaryTransactionId());
        r.setPrimaryPayeeId(alert.getPrimaryPayeeId());
        r.setTotalAmount(alert.getTotalAmount());
        r.setCurrency(alert.getCurrency());
        r.setTransactionCount(alert.getTransactionCount());
        r.setFirstTransactionAt(alert.getFirstTransactionAt());
        r.setLastTransactionAt(alert.getLastTransactionAt());
        r.setResolutionCode(alert.getResolutionCode());
        r.setResolution(alert.getResolution());
        r.setResolutionNotes(alert.getResolutionNotes());
        r.setComment(alert.getComment());
        r.setVersion(alert.getVersion());
        r.setRuleId(alert.getRuleId());
        r.setRuleName(alert.getRuleName());
        r.setRuleType(alert.getRuleType());
        r.setTriggerReason(alert.getTriggerReason());
        r.setActualValue(alert.getActualValue());
        r.setThresholdValue(alert.getThresholdValue());
        r.setTimeWindowMinutes(alert.getTimeWindowMinutes());

        if (includeDetails) {
            // Load status history
            List<AlertHistory> history = alertHistoryRepository.findByAlertAlertIdOrderByChangedAtAsc(alert.getAlertId());
            r.setStatusHistory(history.stream().map(this::toHistoryResponse).collect(Collectors.toList()));

            // Load related transactions
            List<AlertTransaction> ats = alert.getAlertTransactions();
            if (ats != null && !ats.isEmpty()) {
                List<TransactionResponse> txList = ats.stream()
                        .map(at -> transactionRepository.findById(at.getTransactionId()))
                        .filter(opt -> opt.isPresent())
                        .map(opt -> transactionService.toResponse(opt.get()))
                        .collect(Collectors.toList());
                r.setTransactions(txList);
            }
        }

        return r;
    }

    private AlertHistoryResponse toHistoryResponse(AlertHistory h) {
        AlertHistoryResponse r = new AlertHistoryResponse();
        r.setId(h.getId());
        r.setChangedAt(h.getChangedAt());
        r.setActionType(h.getActionType());
        r.setFromStatus(h.getFromStatus());
        r.setToStatus(h.getToStatus());
        r.setComment(h.getComment());
        r.setResolution(h.getResolution());
        r.setChangedBy(h.getChangedBy());
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
