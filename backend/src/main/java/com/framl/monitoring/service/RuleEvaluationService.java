package com.framl.monitoring.service;

import com.framl.monitoring.entity.*;
import com.framl.monitoring.enums.*;
import com.framl.monitoring.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEvaluationService {

    private final RuleRepository ruleRepository;
    private final AlertRepository alertRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluate(Transaction tx) {
        List<Rule> activeRules = ruleRepository.findByActiveTrue();
        RuntimeException evaluationFailure = null;

        for (Rule rule : activeRules) {
            try {
                evaluateRule(tx, rule);
            } catch (Exception e) {
                log.error("Rule evaluation error for rule {} on tx {}: {}", rule.getId(), tx.getTransactionId(), e.getMessage());

                IllegalStateException wrapped = new IllegalStateException(
                    String.format("Rule evaluation failed for rule %s on transaction %s", rule.getId(), tx.getTransactionId()),
                    e
                );
                if (evaluationFailure == null) {
                    evaluationFailure = wrapped;
                } else {
                    evaluationFailure.addSuppressed(wrapped);
                }
            }
        }

        if (evaluationFailure != null) {
            throw evaluationFailure;
        }
    }

    private void evaluateRule(Transaction tx, Rule rule) {
        switch (rule.getRuleType()) {
            case AMOUNT_THRESHOLD -> evaluateAmountThreshold(tx, rule);
            case VELOCITY -> evaluateVelocity(tx, rule);
            case NEW_PAYEE -> evaluateNewPayee(tx, rule);
            case DAILY_LIMIT -> evaluateDailyLimit(tx, rule);
        }
    }

    private void evaluateAmountThreshold(Transaction tx, Rule rule) {
        if (tx.getStatus() != TransactionStatus.COMPLETED) return;
        if (rule.getTransactionTypes() != null && !rule.getTransactionTypes().isEmpty()
                && !rule.getTransactionTypes().contains(tx.getType().name())) return;
        if (rule.getCurrency() != null && !rule.getCurrency().equalsIgnoreCase(tx.getCurrency())) return;
        if (rule.getThreshold() == null) return;

        if (tx.getAmount().compareTo(rule.getThreshold()) > 0) {
            String reason = String.format("Transaction amount %s %s exceeds threshold %s",
                    tx.getAmount(), tx.getCurrency(), rule.getThreshold());
            createAlert(tx, rule, tx.getAmount(), rule.getThreshold(), reason, null,
                tx.getAmount(), 1, tx.getTransactionTime(), tx.getTransactionTime());
        }
    }

    private void evaluateVelocity(Transaction tx, Rule rule) {
        if (tx.getStatus() != TransactionStatus.COMPLETED) return;
        if (rule.getTransactionTypes() != null && !rule.getTransactionTypes().isEmpty()
                && !rule.getTransactionTypes().contains(tx.getType().name())) return;
        if (rule.getTimeWindowMinutes() == null || rule.getMaxTransactionCount() == null) return;

        Instant windowStart = tx.getTransactionTime().minus(rule.getTimeWindowMinutes(), ChronoUnit.MINUTES);
        List<String> types = rule.getTransactionTypes() != null ? rule.getTransactionTypes()
                : Arrays.asList("DEBIT", "CREDIT", "TRANSFER");

        long priorCount = transactionRepository.countVelocityTransactions(
            tx.getAccountId(), types, windowStart, tx.getTransactionTime(), tx.getTransactionId());
        long countWithCurrent = priorCount + 1;
        BigDecimal priorTotalAmount = transactionRepository.sumVelocityAmount(
            tx.getAccountId(), types, windowStart, tx.getTransactionTime(), tx.getTransactionId());
        Instant priorFirstTxTime = transactionRepository.minVelocityTransactionTime(
            tx.getAccountId(), types, windowStart, tx.getTransactionTime(), tx.getTransactionId());
        Instant priorLastTxTime = transactionRepository.maxVelocityTransactionTime(
            tx.getAccountId(), types, windowStart, tx.getTransactionTime(), tx.getTransactionId());

        BigDecimal totalAmountWithCurrent = (priorTotalAmount == null ? BigDecimal.ZERO : priorTotalAmount)
            .add(tx.getAmount());
        Instant firstTxWithCurrent = minInstant(priorFirstTxTime, tx.getTransactionTime());
        Instant lastTxWithCurrent = maxInstant(priorLastTxTime, tx.getTransactionTime());

        if (countWithCurrent > rule.getMaxTransactionCount()) {
            String reason = String.format("%d transactions in %d minutes (max: %d) for account %s",
                countWithCurrent, rule.getTimeWindowMinutes(), rule.getMaxTransactionCount(), tx.getAccountId());
            createAlert(tx, rule, BigDecimal.valueOf(countWithCurrent),
                    BigDecimal.valueOf(rule.getMaxTransactionCount()), reason, rule.getTimeWindowMinutes(),
                    totalAmountWithCurrent, Math.toIntExact(countWithCurrent), firstTxWithCurrent, lastTxWithCurrent);
        }
    }

    private void evaluateNewPayee(Transaction tx, Rule rule) {
        if (tx.getStatus() != TransactionStatus.COMPLETED) return;
        if (tx.getType() != TransactionType.DEBIT) return;
        if (tx.getPayeeId() == null || tx.getPayeeId().isBlank()) return;

        long previousCount = transactionRepository.countPreviousPayeeTransactions(
                tx.getAccountId(), tx.getPayeeId(), tx.getTransactionTime(), tx.getTransactionId());

        if (previousCount == 0) {
            String reason = String.format("First transaction to new payee %s from account %s",
                    tx.getPayeeId(), tx.getAccountId());
            createAlert(tx, rule, BigDecimal.ZERO, BigDecimal.ZERO, reason, null,
                tx.getAmount(), 1, tx.getTransactionTime(), tx.getTransactionTime());
        }
    }

    private void evaluateDailyLimit(Transaction tx, Rule rule) {
        if (tx.getStatus() != TransactionStatus.COMPLETED) return;
        if (tx.getType() != TransactionType.DEBIT) return;
        if (rule.getCurrency() != null && !rule.getCurrency().equalsIgnoreCase(tx.getCurrency())) return;
        if (rule.getDailyLimit() == null) return;

        Instant dayStart = tx.getTransactionTime().atZone(ZoneOffset.UTC)
                .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal dailyTotal = transactionRepository.sumDailyAmount(
            tx.getAccountId(), tx.getCurrency(), dayStart, tx.getTransactionTime(), tx.getTransactionId());
        long priorDailyCount = transactionRepository.countDailyTransactions(
            tx.getAccountId(), tx.getCurrency(), dayStart, tx.getTransactionTime(), tx.getTransactionId());
        Instant priorFirstTxTime = transactionRepository.minDailyTransactionTime(
            tx.getAccountId(), tx.getCurrency(), dayStart, tx.getTransactionTime(), tx.getTransactionId());
        Instant priorLastTxTime = transactionRepository.maxDailyTransactionTime(
            tx.getAccountId(), tx.getCurrency(), dayStart, tx.getTransactionTime(), tx.getTransactionId());

        if (dailyTotal == null) dailyTotal = BigDecimal.ZERO;

        BigDecimal totalWithCurrent = dailyTotal.add(tx.getAmount());
        int countWithCurrent = Math.toIntExact(priorDailyCount + 1);
        Instant firstTxWithCurrent = minInstant(priorFirstTxTime, tx.getTransactionTime());
        Instant lastTxWithCurrent = maxInstant(priorLastTxTime, tx.getTransactionTime());

        if (totalWithCurrent.compareTo(rule.getDailyLimit()) > 0) {
            String reason = String.format("Daily total %s %s exceeds limit %s for account %s",
                totalWithCurrent, tx.getCurrency(), rule.getDailyLimit(), tx.getAccountId());
            createAlert(tx, rule, totalWithCurrent, rule.getDailyLimit(), reason, null,
                totalWithCurrent, countWithCurrent, firstTxWithCurrent, lastTxWithCurrent);
        }
    }

    private void createAlert(Transaction tx, Rule rule, BigDecimal actualValue,
                              BigDecimal thresholdValue, String reason, Integer timeWindow,
                              BigDecimal aggregatedTotalAmount, Integer aggregatedTransactionCount,
                              Instant aggregatedFirstTransactionAt, Instant aggregatedLastTransactionAt) {
        String deduplicationKey = buildDeduplicationKey(tx, rule, timeWindow);
        if (alertRepository.existsByDeduplicationKey(deduplicationKey)) {
            log.debug("Skip duplicate alert by deduplication key {}", deduplicationKey);
            return;
        }

        String alertId = generateAlertId();

        Alert alert = new Alert();
        alert.setAlertId(alertId);
        alert.setDeduplicationKey(deduplicationKey);
        alert.setTitle(rule.getName() + " - " + tx.getAccountId());
        alert.setDescription(reason);
        alert.setAccountId(tx.getAccountId());
        alert.setStatus(AlertStatus.OPEN);
        alert.setSeverity(rule.getSeverity());
        alert.setRiskScore(calculateRiskScore(rule.getSeverity(), actualValue, thresholdValue));
        alert.setPrimaryTransactionId(tx.getTransactionId());
        alert.setPrimaryPayeeId(tx.getPayeeId());
        alert.setTotalAmount(aggregatedTotalAmount != null ? aggregatedTotalAmount : tx.getAmount());
        alert.setCurrency(tx.getCurrency());
        alert.setTransactionCount(aggregatedTransactionCount != null ? aggregatedTransactionCount : 1);
        alert.setFirstTransactionAt(aggregatedFirstTransactionAt != null ? aggregatedFirstTransactionAt : tx.getTransactionTime());
        alert.setLastTransactionAt(aggregatedLastTransactionAt != null ? aggregatedLastTransactionAt : tx.getTransactionTime());
        alert.setRuleId(rule.getId().toString());
        alert.setRuleName(rule.getName());
        alert.setRuleType(rule.getRuleType().name());
        alert.setTriggerReason(reason);
        alert.setActualValue(actualValue);
        alert.setThresholdValue(thresholdValue);
        alert.setTimeWindowMinutes(timeWindow);

        Alert saved;
        try {
            saved = alertRepository.save(alert);
        } catch (DataIntegrityViolationException ex) {
            // Handles race conditions where two threads evaluate the same deduplication key concurrently.
            log.info("Dedup hit on save for key {}. Alert creation skipped.", deduplicationKey);
            return;
        }

        AlertTransaction at = new AlertTransaction();
        at.setAlert(saved);
        at.setTransactionId(tx.getTransactionId());
        at.setPrimaryTrigger(true);
        saved.getAlertTransactions().add(at);
        alertRepository.save(saved);

        // Create history entry
        AlertHistory history = new AlertHistory();
        history.setAlert(saved);
        history.setActionType("CREATED");
        history.setFromStatus(null);
        history.setToStatus(AlertStatus.OPEN);
        history.setComment("Alert created by rule: " + rule.getName());
        alertHistoryRepository.save(history);

        log.info("Alert created: {} for transaction {} by rule {}", alertId, tx.getTransactionId(), rule.getName());
    }

    private Instant minInstant(Instant candidate, Instant fallback) {
        if (candidate == null) {
            return fallback;
        }
        return candidate.isBefore(fallback) ? candidate : fallback;
    }

    private Instant maxInstant(Instant candidate, Instant fallback) {
        if (candidate == null) {
            return fallback;
        }
        return candidate.isAfter(fallback) ? candidate : fallback;
    }

    private int calculateRiskScore(AlertSeverity severity, BigDecimal actual, BigDecimal threshold) {
        int base = switch (severity) {
            case HIGH -> 75;
            case MEDIUM -> 50;
            case LOW -> 25;
        };
        if (threshold != null && threshold.compareTo(BigDecimal.ZERO) > 0 && actual != null) {
            double ratio = actual.doubleValue() / threshold.doubleValue();
            int bonus = (int) Math.min(25, (ratio - 1) * 10);
            return Math.min(100, base + bonus);
        }
        return base;
    }

    private String generateAlertId() {
        return UUID.randomUUID().toString();
    }

    private String buildDeduplicationKey(Transaction tx, Rule rule, Integer timeWindow) {
        if (rule.getRuleType() == RuleType.VELOCITY && timeWindow != null && timeWindow > 0) {
            long epochMinute = tx.getTransactionTime().getEpochSecond() / 60;
            long bucket = epochMinute / timeWindow;
            return String.format("%s|%s|%s|%d",
                    rule.getId(), tx.getAccountId(), rule.getRuleType(), bucket);
        }
        // Option A: one alert per transaction for non-velocity rules.
        return String.format("%s|%s|%s",
                rule.getId(), tx.getTransactionId(), rule.getRuleType());
    }
}
