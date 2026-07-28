package com.framl.monitoring.config;

import com.framl.monitoring.entity.Alert;
import com.framl.monitoring.entity.AlertHistory;
import com.framl.monitoring.entity.AlertTransaction;
import com.framl.monitoring.entity.Transaction;
import com.framl.monitoring.entity.Rule;
import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.AlertStatus;
import com.framl.monitoring.enums.RuleType;
import com.framl.monitoring.enums.TransactionStatus;
import com.framl.monitoring.enums.TransactionType;
import com.framl.monitoring.repository.AlertRepository;
import com.framl.monitoring.repository.RuleRepository;
import com.framl.monitoring.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;

    @Override
    public void run(String... args) {
        if (ruleRepository.count() == 0) {
            log.info("Initializing default monitoring rules...");
            initRules();
        }

        if (transactionRepository.count() == 0 && alertRepository.count() == 0) {
            log.info("Seeding sample transactions and alerts...");
            seedMonitoringData();
        }
    }

    private void initRules() {
        // Amount Threshold Rule
        Rule amountRule = new Rule();
        amountRule.setName("Large Transaction Alert");
        amountRule.setDescription("Alert when a single transaction exceeds $10,000");
        amountRule.setRuleType(RuleType.AMOUNT_THRESHOLD);
        amountRule.setSeverity(AlertSeverity.HIGH);
        amountRule.setActive(true);
        amountRule.setThreshold(new BigDecimal("10000.00"));
        amountRule.setCurrency("USD");
        amountRule.setTransactionTypes(List.of("DEBIT", "TRANSFER"));
        ruleRepository.save(amountRule);

        // Velocity Rule
        Rule velocityRule = new Rule();
        velocityRule.setName("High Frequency Transactions");
        velocityRule.setDescription("Alert when more than 5 transactions occur within 10 minutes from the same account");
        velocityRule.setRuleType(RuleType.VELOCITY);
        velocityRule.setSeverity(AlertSeverity.MEDIUM);
        velocityRule.setActive(true);
        velocityRule.setMaxTransactionCount(5);
        velocityRule.setTimeWindowMinutes(10);
        velocityRule.setTransactionTypes(Arrays.asList("DEBIT", "TRANSFER", "CREDIT"));
        ruleRepository.save(velocityRule);

        // New Payee Rule
        Rule newPayeeRule = new Rule();
        newPayeeRule.setName("New Payee Transaction");
        newPayeeRule.setDescription("Alert on first transaction to a new payee from an account");
        newPayeeRule.setRuleType(RuleType.NEW_PAYEE);
        newPayeeRule.setSeverity(AlertSeverity.LOW);
        newPayeeRule.setActive(true);
        ruleRepository.save(newPayeeRule);

        // Daily Limit Rule
        Rule dailyLimitRule = new Rule();
        dailyLimitRule.setName("Daily Limit Exceeded");
        dailyLimitRule.setDescription("Alert when cumulative daily transactions exceed $50,000");
        dailyLimitRule.setRuleType(RuleType.DAILY_LIMIT);
        dailyLimitRule.setSeverity(AlertSeverity.HIGH);
        dailyLimitRule.setActive(true);
        dailyLimitRule.setDailyLimit(new BigDecimal("50000.00"));
        dailyLimitRule.setCurrency("USD");
        ruleRepository.save(dailyLimitRule);

        log.info("Default rules initialized: 4 rules created.");
    }

        private void seedMonitoringData() {
        Instant now = Instant.now();

        Transaction tx1 = buildTransaction("TXN-9001", "ACC-1001", "PAY-2001", "Blue Horizon Ltd", TransactionType.DEBIT,
            new BigDecimal("18500.00"), "USD", TransactionStatus.COMPLETED, now.minus(6, ChronoUnit.HOURS), "WIRE", "US", "Large supplier payout");
        Transaction tx2 = buildTransaction("TXN-9002", "ACC-1001", "PAY-2002", "North Peak Services", TransactionType.DEBIT,
            new BigDecimal("7400.00"), "USD", TransactionStatus.COMPLETED, now.minus(5, ChronoUnit.HOURS), "CARD", "US", "Recurring service payment");
        Transaction tx3 = buildTransaction("TXN-9003", "ACC-1002", "PAY-2003", "New Harbor Trading", TransactionType.DEBIT,
            new BigDecimal("9200.00"), "USD", TransactionStatus.COMPLETED, now.minus(28, ChronoUnit.HOURS), "API", "GB", "First transfer to new payee");
        Transaction tx4 = buildTransaction("TXN-9004", "ACC-1002", "PAY-2004", "City Logistics BV", TransactionType.DEBIT,
            new BigDecimal("11200.00"), "USD", TransactionStatus.COMPLETED, now.minus(2, ChronoUnit.DAYS), "WEB", "NL", "Cross-border logistics settlement");
        Transaction tx5 = buildTransaction("TXN-9005", "ACC-1003", "PAY-2005", "Acme Wholesale", TransactionType.DEBIT,
            new BigDecimal("6500.00"), "USD", TransactionStatus.COMPLETED, now.minus(3, ChronoUnit.DAYS), "APP", "US", "Batch payment 1");
        Transaction tx6 = buildTransaction("TXN-9006", "ACC-1003", "PAY-2006", "Acme Wholesale", TransactionType.DEBIT,
            new BigDecimal("6800.00"), "USD", TransactionStatus.COMPLETED, now.minus(3, ChronoUnit.DAYS).plus(15, ChronoUnit.MINUTES), "APP", "US", "Batch payment 2");
        Transaction tx7 = buildTransaction("TXN-9007", "ACC-1003", "PAY-2007", "Acme Wholesale", TransactionType.DEBIT,
            new BigDecimal("7100.00"), "USD", TransactionStatus.COMPLETED, now.minus(3, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES), "APP", "US", "Batch payment 3");
        Transaction tx8 = buildTransaction("TXN-9008", "ACC-1004", "PAY-2008", "Sunrise Market", TransactionType.CREDIT,
            new BigDecimal("3200.00"), "USD", TransactionStatus.COMPLETED, now.minus(1, ChronoUnit.DAYS), "BRANCH", "US", "Incoming settlement");
        Transaction tx9 = buildTransaction("TXN-9009", "ACC-1004", "PAY-2009", "Global Services", TransactionType.DEBIT,
            new BigDecimal("54000.00"), "USD", TransactionStatus.COMPLETED, now.minus(12, ChronoUnit.HOURS), "WEB", "US", "Daily limit threshold test");
        Transaction tx10 = buildTransaction("TXN-9010", "ACC-1005", "PAY-2010", "Lunar Imports", TransactionType.DEBIT,
            new BigDecimal("9800.00"), "USD", TransactionStatus.PENDING, now.minus(30, ChronoUnit.MINUTES), "API", "CA", "Pending review");

        transactionRepository.saveAll(List.of(tx1, tx2, tx3, tx4, tx5, tx6, tx7, tx8, tx9, tx10));

        Alert alert1 = buildAlert(seedAlertId(9001), "Large Transaction Alert", "ACC-1001", "PAY-2001", "TXN-9001",
            AlertSeverity.HIGH, AlertStatus.OPEN, 94, now.minus(6, ChronoUnit.HOURS), "Amount above $10,000", "Large Transaction Alert");
        Alert alert2 = buildAlert(seedAlertId(9002), "High Frequency Transactions", "ACC-1003", "PAY-2006", "TXN-9006",
            AlertSeverity.MEDIUM, AlertStatus.INVESTIGATING, 72, now.minus(3, ChronoUnit.DAYS), "Multiple transactions in 10 minutes", "High Frequency Transactions");
        Alert alert3 = buildAlert(seedAlertId(9003), "New Payee Transaction", "ACC-1002", "PAY-2003", "TXN-9003",
            AlertSeverity.LOW, AlertStatus.ACKNOWLEDGED, 41, now.minus(28, ChronoUnit.HOURS), "First debit to a new payee", "New Payee Transaction");
        Alert alert4 = buildAlert(seedAlertId(9004), "Daily Limit Exceeded", "ACC-1004", "PAY-2009", "TXN-9009",
            AlertSeverity.HIGH, AlertStatus.OPEN, 98, now.minus(12, ChronoUnit.HOURS), "Daily debit total above $50,000", "Daily Limit Exceeded");
        Alert alert5 = buildAlert(seedAlertId(9005), "Large Transaction Alert", "ACC-1002", "PAY-2004", "TXN-9004",
            AlertSeverity.HIGH, AlertStatus.INVESTIGATING, 88, now.minus(2, ChronoUnit.DAYS), "Cross-border amount above threshold", "Large Transaction Alert");
        Alert alert6 = buildAlert(seedAlertId(9006), "Large Transaction Alert", "ACC-1001", "PAY-2002", "TXN-9002",
            AlertSeverity.HIGH, AlertStatus.OPEN, 86, now.minus(5, ChronoUnit.HOURS), "Additional high-value debit", "Large Transaction Alert");

        attachHistory(alert1, AlertStatus.OPEN, null, "Created from large-value debit");
        attachHistory(alert2, AlertStatus.INVESTIGATING, AlertStatus.OPEN, "Case escalated for review");
        attachHistory(alert3, AlertStatus.ACKNOWLEDGED, AlertStatus.OPEN, "Analyst acknowledged the case");
        attachHistory(alert4, AlertStatus.OPEN, null, "Daily limit threshold breached");
        attachHistory(alert5, AlertStatus.INVESTIGATING, AlertStatus.OPEN, "Cross-border pattern identified");
        attachHistory(alert6, AlertStatus.OPEN, null, "Second large transaction detected");

        attachTransaction(alert1, tx1, true);
        attachTransaction(alert2, tx5, true);
        attachTransaction(alert2, tx6, false);
        attachTransaction(alert2, tx7, false);
        attachTransaction(alert3, tx3, true);
        attachTransaction(alert4, tx9, true);
        attachTransaction(alert5, tx4, true);
        attachTransaction(alert6, tx2, true);

        alertRepository.saveAll(List.of(alert1, alert2, alert3, alert4, alert5, alert6));
        log.info("Sample monitoring data initialized: {} transactions, {} alerts.", transactionRepository.count(), alertRepository.count());
        }

        private Transaction buildTransaction(String transactionId, String accountId, String payeeId, String payeeName,
                         TransactionType type, BigDecimal amount, String currency,
                         TransactionStatus status, Instant transactionTime,
                         String channel, String country, String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setAccountId(accountId);
        transaction.setPayeeId(payeeId);
        transaction.setPayeeName(payeeName);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setStatus(status);
        transaction.setTransactionTime(transactionTime);
        transaction.setReceivedAt(transactionTime.plus(2, ChronoUnit.MINUTES));
        transaction.setEvaluatedAt(transactionTime.plus(3, ChronoUnit.MINUTES));
        transaction.setEvaluationMode("REAL_TIME");
        transaction.setPaymentChannel(channel);
        transaction.setCountry(country);
        transaction.setDescription(description);
        transaction.setVersion(1);
        transaction.setLateArrival(false);
        return transaction;
        }

        private Alert buildAlert(String alertId, String title, String accountId, String payeeId, String transactionId,
                     AlertSeverity severity, AlertStatus status, Integer riskScore,
                     Instant createdAt, String triggerReason, String ruleName) {
        Alert alert = new Alert();
        alert.setAlertId(alertId);
        alert.setTitle(title);
        alert.setDescription(triggerReason);
        alert.setAccountId(accountId);
        alert.setPrimaryPayeeId(payeeId);
        alert.setPrimaryTransactionId(transactionId);
        alert.setStatus(status);
        alert.setSeverity(severity);
        alert.setRiskScore(riskScore);
        alert.setCreatedAt(createdAt);
        alert.setUpdatedAt(createdAt.plus(5, ChronoUnit.MINUTES));
        alert.setAcknowledgedAt(status == AlertStatus.ACKNOWLEDGED ? createdAt.plus(15, ChronoUnit.MINUTES) : null);
        alert.setInvestigatingAt(status == AlertStatus.INVESTIGATING ? createdAt.plus(20, ChronoUnit.MINUTES) : null);
        alert.setCurrency("USD");
        alert.setTransactionCount(1);
        alert.setFirstTransactionAt(createdAt.minus(30, ChronoUnit.MINUTES));
        alert.setLastTransactionAt(createdAt);
        alert.setRuleName(ruleName);
        alert.setRuleType("AUTO");
        alert.setTriggerReason(triggerReason);
        alert.setVersion(1);
        return alert;
        }

        private String seedAlertId(int seed) {
        return String.format("ALT-00000000-0000-0000-0000-%012d", seed);
        }

        private void attachHistory(Alert alert, AlertStatus toStatus, AlertStatus fromStatus, String comment) {
        AlertHistory history = new AlertHistory();
        history.setAlert(alert);
        history.setActionType("STATUS_CHANGED");
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setComment(comment);
        alert.getStatusHistory().add(history);
        }

        private void attachTransaction(Alert alert, Transaction transaction, boolean primary) {
        AlertTransaction alertTransaction = new AlertTransaction();
        alertTransaction.setAlert(alert);
        alertTransaction.setTransactionId(transaction.getTransactionId());
        alertTransaction.setPrimaryTrigger(primary);
        alert.getAlertTransactions().add(alertTransaction);
        }
}
