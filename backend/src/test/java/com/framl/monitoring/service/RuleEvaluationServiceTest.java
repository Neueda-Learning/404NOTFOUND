package com.framl.monitoring.service;

import com.framl.monitoring.entity.*;
import com.framl.monitoring.enums.*;
import com.framl.monitoring.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleEvaluationServiceTest {

    private RuleEvaluationService ruleEvaluationService;

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        ruleEvaluationService = new RuleEvaluationService(ruleRepository, alertRepository, 
                                                          alertHistoryRepository, transactionRepository);
    }

    @Test
    void evaluate_processesAllActiveRulesForTransaction() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(5000), 
                                              TransactionStatus.COMPLETED);
        Rule rule1 = createTestRule(1L, RuleType.AMOUNT_THRESHOLD, true, BigDecimal.valueOf(4000));
        Rule rule2 = createTestRule(2L, RuleType.VELOCITY, true, null);

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule1, rule2));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        ruleEvaluationService.evaluate(tx);

        verify(ruleRepository).findByActiveTrue();
        verify(alertRepository, atLeastOnce()).save(any(Alert.class));
    }

    @Test
    void evaluate_skipsInactiveRules() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(5000), 
                                              TransactionStatus.COMPLETED);

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of());

        ruleEvaluationService.evaluate(tx);

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void evaluate_continuesOnRuleEvaluationError() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(5000), 
                                              TransactionStatus.COMPLETED);
        Rule rule1 = createTestRule(1L, RuleType.AMOUNT_THRESHOLD, true, BigDecimal.valueOf(4000));
        Rule rule2 = createTestRule(2L, RuleType.AMOUNT_THRESHOLD, true, BigDecimal.valueOf(6000));

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule1, rule2));
        when(alertRepository.save(any(Alert.class)))
                .thenThrow(new RuntimeException("Database error"))
                .thenReturn(new Alert());

        // Should not throw, continues processing despite error
        assertDoesNotThrow(() -> ruleEvaluationService.evaluate(tx));
        
        // At least 1 attempt was made (the second rule's save succeeded)
        verify(alertRepository, atLeastOnce()).save(any(Alert.class));
    }

    @Test
    void evaluateAmountThreshold_createsAlertWhenExceeded() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(5000), 
                                              TransactionStatus.COMPLETED);
        Rule rule = createTestRule(1L, RuleType.AMOUNT_THRESHOLD, true, BigDecimal.valueOf(4000));

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        ruleEvaluationService.evaluate(tx);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, atLeastOnce()).save(alertCaptor.capture());
        
        Alert savedAlert = alertCaptor.getValue();
        assertEquals("ACC-001", savedAlert.getAccountId());
        assertEquals("TXN-001", savedAlert.getPrimaryTransactionId());
    }

    @Test
    void evaluateAmountThreshold_ignoresIncompleteTransaction() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(5000), 
                                              TransactionStatus.PENDING);
        Rule rule = createTestRule(1L, RuleType.AMOUNT_THRESHOLD, true, BigDecimal.valueOf(4000));

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));

        ruleEvaluationService.evaluate(tx);

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void evaluateAmountThreshold_respectsTransactionTypeFilter() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(5000), 
                                              TransactionStatus.COMPLETED);
        tx.setType(TransactionType.DEBIT);
        
        Rule rule = createTestRule(1L, RuleType.AMOUNT_THRESHOLD, true, BigDecimal.valueOf(4000));
        rule.setTransactionTypes(List.of("CREDIT")); // Only CREDIT type

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));

        ruleEvaluationService.evaluate(tx);

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void evaluateAmountThreshold_respectsCurrencyFilter() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(5000), 
                                              TransactionStatus.COMPLETED);
        tx.setCurrency("USD");
        
        Rule rule = createTestRule(1L, RuleType.AMOUNT_THRESHOLD, true, BigDecimal.valueOf(4000));
        rule.setCurrency("EUR"); // Only EUR

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));

        ruleEvaluationService.evaluate(tx);

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void evaluateVelocity_createsAlertWhenExceeded() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(500), 
                                              TransactionStatus.COMPLETED);
        
        Rule rule = createTestRule(1L, RuleType.VELOCITY, true, null);
        rule.setTimeWindowMinutes(60);
        rule.setMaxTransactionCount(5);

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(transactionRepository.countVelocityTransactions(
                eq("ACC-001"), anyList(), any(Instant.class), any(Instant.class)))
                .thenReturn(6L); // 6 transactions > 5 max
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        ruleEvaluationService.evaluate(tx);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, atLeastOnce()).save(alertCaptor.capture());
        
        Alert alert = alertCaptor.getValue();
        assertTrue(alert.getTriggerReason().contains("6 transactions"));
        assertEquals(60, alert.getTimeWindowMinutes());
    }

    @Test
    void evaluateVelocity_ignoresWhenBelowThreshold() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(500), 
                                              TransactionStatus.COMPLETED);
        
        Rule rule = createTestRule(1L, RuleType.VELOCITY, true, null);
        rule.setTimeWindowMinutes(60);
        rule.setMaxTransactionCount(10);

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(transactionRepository.countVelocityTransactions(
                eq("ACC-001"), anyList(), any(Instant.class), any(Instant.class)))
                .thenReturn(5L); // 5 <= 10 max

        ruleEvaluationService.evaluate(tx);

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void evaluateNewPayee_createsAlertForFirstTransactionToNewPayee() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(500), 
                                              TransactionStatus.COMPLETED);
        tx.setType(TransactionType.DEBIT);
        tx.setPayeeId("PAYEE-NEW-001");

        Rule rule = createTestRule(1L, RuleType.NEW_PAYEE, true, null);

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(transactionRepository.countPreviousPayeeTransactions(
                "ACC-001", "PAYEE-NEW-001", tx.getTransactionTime(), "TXN-001"))
                .thenReturn(0L); // No previous transactions
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        ruleEvaluationService.evaluate(tx);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, atLeastOnce()).save(alertCaptor.capture());
        
        Alert alert = alertCaptor.getValue();
        assertTrue(alert.getTriggerReason().contains("new payee"));
    }

    @Test
    void evaluateNewPayee_ignoresNonDebitTransactions() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(500), 
                                              TransactionStatus.COMPLETED);
        tx.setType(TransactionType.CREDIT); // Not DEBIT
        tx.setPayeeId("PAYEE-001");

        Rule rule = createTestRule(1L, RuleType.NEW_PAYEE, true, null);

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));

        ruleEvaluationService.evaluate(tx);

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void evaluateNewPayee_ignoresWhenPayeeIdMissing() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(500), 
                                              TransactionStatus.COMPLETED);
        tx.setType(TransactionType.DEBIT);
        tx.setPayeeId(null); // Missing payee

        Rule rule = createTestRule(1L, RuleType.NEW_PAYEE, true, null);

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));

        ruleEvaluationService.evaluate(tx);

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void evaluateDailyLimit_createsAlertWhenExceeded() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(5000), 
                                              TransactionStatus.COMPLETED);
        tx.setType(TransactionType.DEBIT);
        tx.setCurrency("USD");

        Rule rule = createTestRule(1L, RuleType.DAILY_LIMIT, true, null);
        rule.setDailyLimit(BigDecimal.valueOf(10000));

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(transactionRepository.sumDailyAmount(
                eq("ACC-001"), eq("USD"), any(Instant.class), any(Instant.class)))
                .thenReturn(BigDecimal.valueOf(15000)); // Exceeds 10000 limit
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        ruleEvaluationService.evaluate(tx);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, atLeastOnce()).save(alertCaptor.capture());
        
        Alert alert = alertCaptor.getValue();
        assertTrue(alert.getTriggerReason().contains("Daily total"));
    }

    @Test
    void evaluateDailyLimit_ignoresNonDebitTransactions() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(5000), 
                                              TransactionStatus.COMPLETED);
        tx.setType(TransactionType.CREDIT); // Not DEBIT

        Rule rule = createTestRule(1L, RuleType.DAILY_LIMIT, true, null);

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));

        ruleEvaluationService.evaluate(tx);

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void createAlert_setsCorrectAlertProperties() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(5000), 
                                              TransactionStatus.COMPLETED);
        Rule rule = createTestRule(1L, RuleType.AMOUNT_THRESHOLD, true, BigDecimal.valueOf(4000));

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        ruleEvaluationService.evaluate(tx);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, atLeastOnce()).save(alertCaptor.capture());
        
        Alert alert = alertCaptor.getValue();
        assertNotNull(alert.getAlertId());
        assertTrue(alert.getAlertId().contains("ALT-"));
        assertEquals("Test Rule - ACC-001", alert.getTitle());
        assertEquals("TXN-001", alert.getPrimaryTransactionId());
        assertEquals(BigDecimal.valueOf(5000), alert.getTotalAmount());
        assertEquals("USD", alert.getCurrency());
        assertEquals(1, alert.getTransactionCount());
        assertEquals("1", alert.getRuleId());
        assertEquals("Test Rule", alert.getRuleName());
    }

    @Test
    void createAlert_calculatesRiskScoreBasedOnSeverity() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(5000), 
                                              TransactionStatus.COMPLETED);
        Rule highSeverityRule = createTestRule(1L, RuleType.AMOUNT_THRESHOLD, true, BigDecimal.valueOf(4000));
        highSeverityRule.setSeverity(AlertSeverity.HIGH);

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(highSeverityRule));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        ruleEvaluationService.evaluate(tx);

        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, atLeastOnce()).save(alertCaptor.capture());
        
        Alert alert = alertCaptor.getValue();
        assertTrue(alert.getRiskScore() >= 75); // HIGH severity base = 75
    }

    @Test
    void createAlert_createsHistoryEntry() {
        Transaction tx = createTestTransaction("TXN-001", "ACC-001", BigDecimal.valueOf(5000), 
                                              TransactionStatus.COMPLETED);
        Rule rule = createTestRule(1L, RuleType.AMOUNT_THRESHOLD, true, BigDecimal.valueOf(4000));

        when(ruleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        ruleEvaluationService.evaluate(tx);

        verify(alertHistoryRepository).save(any(AlertHistory.class));
    }

    // Helper methods to create test objects
    private Transaction createTestTransaction(String txId, String accountId, BigDecimal amount, 
                                             TransactionStatus status) {
        Transaction tx = new Transaction();
        tx.setTransactionId(txId);
        tx.setAccountId(accountId);
        tx.setAmount(amount);
        tx.setCurrency("USD");
        tx.setStatus(status);
        tx.setType(TransactionType.TRANSFER);
        tx.setTransactionTime(Instant.now());
        tx.setVersion(1);
        return tx;
    }

    private Rule createTestRule(Long ruleId, RuleType ruleType, boolean active, BigDecimal threshold) {
        Rule rule = new Rule();
        rule.setId(ruleId);
        rule.setName("Test Rule");
        rule.setRuleType(ruleType);
        rule.setActive(active);
        rule.setSeverity(AlertSeverity.MEDIUM);
        rule.setThreshold(threshold);
        rule.setTransactionTypes(null);
        return rule;
    }
}
