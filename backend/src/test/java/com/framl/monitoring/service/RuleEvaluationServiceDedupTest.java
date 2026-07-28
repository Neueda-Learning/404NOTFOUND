package com.framl.monitoring.service;

import com.framl.monitoring.entity.Rule;
import com.framl.monitoring.entity.Transaction;
import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.RuleType;
import com.framl.monitoring.enums.TransactionStatus;
import com.framl.monitoring.enums.TransactionType;
import com.framl.monitoring.repository.AlertHistoryRepository;
import com.framl.monitoring.repository.AlertRepository;
import com.framl.monitoring.repository.RuleRepository;
import com.framl.monitoring.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEvaluationServiceDedupTest {

    @Mock
    private RuleRepository ruleRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AlertHistoryRepository alertHistoryRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private RuleEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new RuleEvaluationService(ruleRepository, alertRepository, alertHistoryRepository, transactionRepository);
    }

    @Test
    void generatesUuidAndDeduplicatesOnRetryForSameTransaction() {
        Rule rule = amountThresholdRule();
        Transaction tx = completedDebitTransaction();

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(alertRepository.existsByDeduplicationKey(anyString())).thenReturn(false, true);
        when(alertRepository.save(any(com.framl.monitoring.entity.Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.evaluate(tx);
        service.evaluate(tx); // retry for same transaction

        ArgumentCaptor<com.framl.monitoring.entity.Alert> captor =
                ArgumentCaptor.forClass(com.framl.monitoring.entity.Alert.class);
        verify(alertRepository, times(2)).existsByDeduplicationKey(anyString());
        verify(alertRepository, times(2)).save(captor.capture());
        verify(alertHistoryRepository, times(1)).save(any());

        com.framl.monitoring.entity.Alert created = captor.getAllValues().get(0);
        assertNotNull(created.getAlertId());
        assertTrue(created.getAlertId().matches("^[0-9a-fA-F-]{36}$"));
        assertTrue(created.getDeduplicationKey().startsWith("101|TX-001|AMOUNT_THRESHOLD"));
    }

    @Test
    void swallowsDuplicateKeyExceptionUnderConcurrentInsertRace() {
        Rule rule = velocityRule();
        Transaction tx = completedDebitTransaction();

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(transactionRepository.countVelocityTransactions(anyString(), any(), any(), any(), anyString())).thenReturn(5L);
        when(transactionRepository.sumVelocityAmount(anyString(), any(), any(), any(), anyString())).thenReturn(new BigDecimal("3000.00"));
        when(transactionRepository.minVelocityTransactionTime(anyString(), any(), any(), any(), anyString()))
            .thenReturn(Instant.parse("2026-07-28T09:55:00Z"));
        when(transactionRepository.maxVelocityTransactionTime(anyString(), any(), any(), any(), anyString()))
            .thenReturn(Instant.parse("2026-07-28T09:59:00Z"));
        when(alertRepository.existsByDeduplicationKey(anyString())).thenReturn(false);
        when(alertRepository.save(any(com.framl.monitoring.entity.Alert.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate dedup key"));

        service.evaluate(tx);

        verify(alertRepository, times(1)).existsByDeduplicationKey(anyString());
        verify(alertRepository, times(1)).save(any(com.framl.monitoring.entity.Alert.class));
        verify(alertHistoryRepository, never()).save(any());
    }

    @Test
    void propagatesRuleEvaluationExceptionInsteadOfSwallowing() {
        Rule rule = velocityRule();
        Transaction tx = completedDebitTransaction();

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(transactionRepository.countVelocityTransactions(anyString(), any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("simulated repository failure"));

        assertThrows(IllegalStateException.class, () -> service.evaluate(tx));

        verify(alertRepository, never()).save(any(com.framl.monitoring.entity.Alert.class));
    }

    @Test
    void velocityAggregateExcludesCurrentTransactionFromQuery() {
        Rule rule = velocityRule();
        Transaction tx = completedDebitTransaction();

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(transactionRepository.countVelocityTransactions(anyString(), any(), any(), any(), anyString())).thenReturn(3L);
        when(transactionRepository.sumVelocityAmount(anyString(), any(), any(), any(), anyString())).thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.minVelocityTransactionTime(anyString(), any(), any(), any(), anyString()))
            .thenReturn(Instant.parse("2026-07-28T09:52:00Z"));
        when(transactionRepository.maxVelocityTransactionTime(anyString(), any(), any(), any(), anyString()))
            .thenReturn(Instant.parse("2026-07-28T09:59:00Z"));
        when(alertRepository.existsByDeduplicationKey(anyString())).thenReturn(false);
        when(alertRepository.save(any(com.framl.monitoring.entity.Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.evaluate(tx);

        verify(transactionRepository, times(1)).countVelocityTransactions(
                eq("ACC-001"), any(), any(), eq(Instant.parse("2026-07-28T10:00:00Z")), eq("TX-001"));
        verify(transactionRepository, times(1)).sumVelocityAmount(
            eq("ACC-001"), any(), any(), eq(Instant.parse("2026-07-28T10:00:00Z")), eq("TX-001"));
        verify(alertRepository, times(2)).save(any(com.framl.monitoring.entity.Alert.class));
    }

    @Test
    void dailyLimitAggregateExcludesCurrentTransactionFromQuery() {
        Rule rule = dailyLimitRule();
        Transaction tx = completedDebitTransaction();

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(transactionRepository.sumDailyAmount(anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(new BigDecimal("50000.00"));
        when(transactionRepository.countDailyTransactions(anyString(), anyString(), any(), any(), anyString()))
            .thenReturn(5L);
        when(transactionRepository.minDailyTransactionTime(anyString(), anyString(), any(), any(), anyString()))
            .thenReturn(Instant.parse("2026-07-28T01:00:00Z"));
        when(transactionRepository.maxDailyTransactionTime(anyString(), anyString(), any(), any(), anyString()))
            .thenReturn(Instant.parse("2026-07-28T09:50:00Z"));
        when(alertRepository.existsByDeduplicationKey(anyString())).thenReturn(false);
        when(alertRepository.save(any(com.framl.monitoring.entity.Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.evaluate(tx);

        verify(transactionRepository, times(1)).sumDailyAmount(
                eq("ACC-001"), eq("USD"), any(), eq(Instant.parse("2026-07-28T10:00:00Z")), eq("TX-001"));
        verify(transactionRepository, times(1)).countDailyTransactions(
            eq("ACC-001"), eq("USD"), any(), eq(Instant.parse("2026-07-28T10:00:00Z")), eq("TX-001"));
        verify(alertRepository, times(2)).save(any(com.framl.monitoring.entity.Alert.class));
    }

    private static Rule amountThresholdRule() {
        Rule rule = new Rule();
        rule.setId(101L);
        rule.setName("Large Amount");
        rule.setRuleType(RuleType.AMOUNT_THRESHOLD);
        rule.setSeverity(AlertSeverity.HIGH);
        rule.setThreshold(new BigDecimal("1000.00"));
        rule.setTransactionTypes(List.of("DEBIT"));
        rule.setCurrency("USD");
        return rule;
    }

    private static Rule velocityRule() {
        Rule rule = new Rule();
        rule.setId(202L);
        rule.setName("Velocity");
        rule.setRuleType(RuleType.VELOCITY);
        rule.setSeverity(AlertSeverity.MEDIUM);
        rule.setTransactionTypes(List.of("DEBIT"));
        rule.setMaxTransactionCount(3);
        rule.setTimeWindowMinutes(10);
        return rule;
    }

    private static Rule dailyLimitRule() {
        Rule rule = new Rule();
        rule.setId(303L);
        rule.setName("Daily Limit");
        rule.setRuleType(RuleType.DAILY_LIMIT);
        rule.setSeverity(AlertSeverity.HIGH);
        rule.setCurrency("USD");
        rule.setDailyLimit(new BigDecimal("51000.00"));
        return rule;
    }

    private static Transaction completedDebitTransaction() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TX-001");
        tx.setAccountId("ACC-001");
        tx.setPayeeId("PAY-001");
        tx.setType(TransactionType.DEBIT);
        tx.setAmount(new BigDecimal("1500.00"));
        tx.setCurrency("USD");
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setTransactionTime(Instant.parse("2026-07-28T10:00:00Z"));
        tx.setReceivedAt(Instant.parse("2026-07-28T10:00:30Z"));
        return tx;
    }
}
