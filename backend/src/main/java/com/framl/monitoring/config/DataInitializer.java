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

import com.framl.monitoring.enums.ResolutionCode;

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

    // Fixed timestamps for July 26-27, 2026
    private static final Instant T26_08 = Instant.parse("2026-07-26T08:00:00Z");
    private static final Instant T26_09 = Instant.parse("2026-07-26T09:00:00Z");
    private static final Instant T26_10 = Instant.parse("2026-07-26T10:00:00Z");
    private static final Instant T26_11 = Instant.parse("2026-07-26T11:00:00Z");
    private static final Instant T26_12 = Instant.parse("2026-07-26T12:00:00Z");
    private static final Instant T26_13 = Instant.parse("2026-07-26T13:00:00Z");
    private static final Instant T26_14 = Instant.parse("2026-07-26T14:00:00Z");
    private static final Instant T26_15 = Instant.parse("2026-07-26T15:00:00Z");
    private static final Instant T26_16 = Instant.parse("2026-07-26T16:00:00Z");
    private static final Instant T26_17 = Instant.parse("2026-07-26T17:00:00Z");
    private static final Instant T26_18 = Instant.parse("2026-07-26T18:00:00Z");
    private static final Instant T27_08 = Instant.parse("2026-07-27T08:00:00Z");
    private static final Instant T27_09 = Instant.parse("2026-07-27T09:00:00Z");
    private static final Instant T27_10 = Instant.parse("2026-07-27T10:00:00Z");
    private static final Instant T27_11 = Instant.parse("2026-07-27T11:00:00Z");
    private static final Instant T27_12 = Instant.parse("2026-07-27T12:00:00Z");
    private static final Instant T27_13 = Instant.parse("2026-07-27T13:00:00Z");
    private static final Instant T27_14 = Instant.parse("2026-07-27T14:00:00Z");
    private static final Instant T27_15 = Instant.parse("2026-07-27T15:00:00Z");
    private static final Instant T27_16 = Instant.parse("2026-07-27T16:00:00Z");
    private static final Instant T27_17 = Instant.parse("2026-07-27T17:00:00Z");

    private void seedMonitoringData() {
        // ═══════════════════════════════════════════════════════════════
        // Transactions — 20 transactions on July 26 & 27, 2026
        // Covers all TransactionType, TransactionStatus, channels, currencies
        // ═══════════════════════════════════════════════════════════════

        // ── 7/26: ACC-101 large transfer + ACC-100 normal txn ──
        Transaction t1 = buildTx("TXN-D001", "ACC-101", "PAY-B01", "Shanghai Trading Corp", TransactionType.TRANSFER,
            bd("45000.00"), "USD", TransactionStatus.COMPLETED, T26_08, "WIRE", "CN", "Supply chain settlement — large cross-border wire");
        Transaction t2 = buildTx("TXN-D002", "ACC-100", "PAY-A02", "Office Supplies Co", TransactionType.DEBIT,
            bd("2450.00"), "USD", TransactionStatus.COMPLETED, T26_09, "CARD", "US", "Office equipment purchase");

        // ── 7/26: ACC-102 velocity batch (6 txns in 10 min window) ──
        Transaction t3 = buildTx("TXN-D003", "ACC-102", "PAY-C01", "Quick Mart 1", TransactionType.DEBIT,
            bd("550.00"), "USD", TransactionStatus.COMPLETED, T26_10, "POS", "US", "POS purchase — convenience store");
        Transaction t4 = buildTx("TXN-D004", "ACC-102", "PAY-C02", "Quick Mart 2", TransactionType.DEBIT,
            bd("675.00"), "USD", TransactionStatus.COMPLETED, T26_10.plus(2, ChronoUnit.MINUTES), "POS", "US", "POS purchase — gas station");
        Transaction t5 = buildTx("TXN-D005", "ACC-102", "PAY-C03", "Quick Mart 3", TransactionType.DEBIT,
            bd("820.00"), "USD", TransactionStatus.COMPLETED, T26_10.plus(4, ChronoUnit.MINUTES), "POS", "US", "POS purchase — electronics store");
        Transaction t6 = buildTx("TXN-D006", "ACC-102", "PAY-C04", "Quick Mart 4", TransactionType.DEBIT,
            bd("430.00"), "USD", TransactionStatus.COMPLETED, T26_10.plus(6, ChronoUnit.MINUTES), "POS", "US", "POS purchase — pharmacy");
        Transaction t7 = buildTx("TXN-D007", "ACC-102", "PAY-C05", "Quick Mart 5", TransactionType.DEBIT,
            bd("920.00"), "USD", TransactionStatus.COMPLETED, T26_10.plus(8, ChronoUnit.MINUTES), "POS", "US", "POS purchase — supermarket");
        Transaction t8 = buildTx("TXN-D008", "ACC-102", "PAY-C06", "Quick Mart 6", TransactionType.DEBIT,
            bd("700.00"), "USD", TransactionStatus.COMPLETED, T26_10.plus(9, ChronoUnit.MINUTES), "POS", "US", "POS purchase — liquor store");

        // ── 7/26: ACC-103 daily limit batch (>$50k in one day) ──
        Transaction t9 = buildTx("TXN-D009", "ACC-103", "PAY-D01", "Real Estate Holdings", TransactionType.DEBIT,
            bd("25000.00"), "USD", TransactionStatus.COMPLETED, T26_11, "WIRE", "US", "Real estate down payment — first installment");
        Transaction t10 = buildTx("TXN-D010", "ACC-103", "PAY-D02", "Luxury Auto Dealer", TransactionType.DEBIT,
            bd("18000.00"), "USD", TransactionStatus.COMPLETED, T26_12, "WIRE", "US", "Luxury vehicle purchase — bank wire");
        Transaction t11 = buildTx("TXN-D011", "ACC-103", "PAY-D03", "Investment Fund XYZ", TransactionType.DEBIT,
            bd("15000.00"), "USD", TransactionStatus.COMPLETED, T26_13, "WIRE", "KY", "Offshore fund subscription — Cayman Islands");

        // ── 7/26: New payee + CREDIT ──
        Transaction t12 = buildTx("TXN-D012", "ACC-104", "PAY-NEW-01", "Unknown Merchant Alpha", TransactionType.DEBIT,
            bd("4800.00"), "USD", TransactionStatus.COMPLETED, T26_14, "ONLINE", "MT", "First-time crypto purchase from unknown exchange");
        Transaction t13 = buildTx("TXN-D013", "ACC-100", "PAY-A03", "Salary Inc", TransactionType.CREDIT,
            bd("8500.00"), "USD", TransactionStatus.COMPLETED, T26_16, "ACH", "US", "Monthly salary deposit — employer ACH credit");

        // ── 7/27: Large cross-border txn (GBP, EUR, JPY) ──
        Transaction t14 = buildTx("TXN-D014", "ACC-106", "PAY-F01", "London Consulting Ltd", TransactionType.TRANSFER,
            bd("22000.00"), "GBP", TransactionStatus.COMPLETED, T27_08, "WIRE", "GB", "UK consulting fee — cross-border wire transfer");
        Transaction t15 = buildTx("TXN-D015", "ACC-106", "PAY-F02", "Tokyo Suppliers KK", TransactionType.DEBIT,
            bd("15000.00"), "JPY", TransactionStatus.COMPLETED, T27_09, "WIRE", "JP", "Electronics components import payment");
        Transaction t16 = buildTx("TXN-D016", "ACC-106", "PAY-F03", "Paris Fashion House", TransactionType.DEBIT,
            bd("35000.00"), "EUR", TransactionStatus.COMPLETED, T27_10, "WIRE", "FR", "Seasonal fashion collection — bulk order");

        // ── 7/27: Non-COMPLETED transactions ──
        Transaction t17 = buildTx("TXN-D017", "ACC-105", "PAY-E01", "Electronics Store DE", TransactionType.DEBIT,
            bd("1299.00"), "EUR", TransactionStatus.PENDING, T27_14, "ONLINE", "DE", "Online order — pending bank authorization");
        Transaction t18 = buildTx("TXN-D018", "ACC-105", "PAY-E02", "Travel Agency FR", TransactionType.DEBIT,
            bd("4200.00"), "EUR", TransactionStatus.FAILED, T27_11, "ONLINE", "FR", "Flight booking — insufficient funds, payment rejected");
        Transaction t19 = buildTx("TXN-D019", "ACC-105", "PAY-E03", "Streaming Service GmbH", TransactionType.DEBIT,
            bd("29.99"), "EUR", TransactionStatus.CANCELLED, T27_12, "ONLINE", "DE", "Monthly subscription — user cancelled before settlement");
        Transaction t20 = buildTx("TXN-D020", "ACC-105", "PAY-E04", "Suspicious Merchant IT", TransactionType.DEBIT,
            bd("8900.00"), "EUR", TransactionStatus.REVERSED, T27_13, "POS", "IT", "Card-present transaction — chargeback reversal by issuing bank");

        transactionRepository.saveAll(List.of(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10,
            t11, t12, t13, t14, t15, t16, t17, t18, t19, t20));

        // ═══════════════════════════════════════════════════════════════
        // Alerts — 16 alerts covering ALL statuses, severities, rule types,
        //           resolution codes
        //           Each with complete, readable lifecycle history
        // ═══════════════════════════════════════════════════════════════

        // ===== 7/26 ALERTS (10) =====

        // ── ALT-2601: OPEN — AMOUNT_THRESHOLD triggered by 45k wire ──
        Alert a1 = buildAlert("ALT-2601", "Large Transaction Alert - ACC-101", "ACC-101", "PAY-B01", "TXN-D001",
            AlertSeverity.HIGH, AlertStatus.OPEN, 96, T26_08,
            "Transaction amount $45,000.00 exceeds monitoring threshold $10,000.00, triggered large transaction rule",
            "Large Transaction Alert", RuleType.AMOUNT_THRESHOLD, bd("45000.00"), bd("10000.00"), bd("45000.00"), "USD");

        // ── ALT-2602: OPEN — VELOCITY 6 txns/10 min ──
        Alert a2 = buildAlert("ALT-2602", "High Frequency Alert - ACC-102", "ACC-102", "PAY-C06", "TXN-D008",
            AlertSeverity.MEDIUM, AlertStatus.OPEN, 68, T26_10.plus(10, ChronoUnit.MINUTES),
            "Account ACC-102 had 6 transactions in 10 minutes (limit 5), suspected structuring to evade monitoring",
            "High Frequency Transactions", RuleType.VELOCITY, bd("6"), bd("5"), bd("4095.00"), "USD");

        // ── ALT-2603: ACKNOWLEDGED → confirmed pending investigation — NEW_PAYEE ──
        Alert a3 = buildAlert("ALT-2603", "New Payee Alert - ACC-104", "ACC-104", "PAY-NEW-01", "TXN-D012",
            AlertSeverity.LOW, AlertStatus.ACKNOWLEDGED, 25, T26_14,
            "Account ACC-104 initiated first transaction to payee Unknown Merchant Alpha, triggered new payee rule",
            "New Payee Transaction", RuleType.NEW_PAYEE, BigDecimal.ZERO, BigDecimal.ZERO, bd("4800.00"), "USD");
        a3.setAcknowledgedAt(T26_15);
        a3.setComment("Analyst Julie acknowledged — account has multiple new payees recently, requires ongoing monitoring");

        // ── ALT-2604: INVESTIGATING — DAILY_LIMIT 3 txns total >$50k ──
        Alert a4 = buildAlert("ALT-2604", "Daily Limit Alert - ACC-103", "ACC-103", "PAY-D03", "TXN-D011",
            AlertSeverity.HIGH, AlertStatus.INVESTIGATING, 92, T26_13.plus(5, ChronoUnit.MINUTES),
            "Account ACC-103 daily cumulative $58,000.00 exceeds daily limit $50,000.00, involving real estate, luxury car, and offshore fund wire transfers",
            "Daily Limit Exceeded", RuleType.DAILY_LIMIT, bd("58000.00"), bd("50000.00"), bd("58000.00"), "USD");
        a4.setAcknowledgedAt(T26_14);
        a4.setInvestigatingAt(T26_16);
        a4.setComment("Analyst Lucas investigating — three wire transfers to dispersed payees, possible structured splitting");

        // ── ALT-2605: INVESTIGATING — AMOUNT_THRESHOLD on CREDIT ──
        Alert a5 = buildAlert("ALT-2605", "Large Deposit Alert - ACC-100", "ACC-100", "PAY-A03", "TXN-D013",
            AlertSeverity.MEDIUM, AlertStatus.INVESTIGATING, 55, T26_16.plus(5, ChronoUnit.MINUTES),
            "Account ACC-100 received $8,500.00 ACH deposit from Salary Inc, but amount does not match historical salary records",
            "Large Transaction Alert", RuleType.AMOUNT_THRESHOLD, bd("8500.00"), bd("10000.00"), bd("8500.00"), "USD");
        a5.setAcknowledgedAt(T26_17);
        a5.setInvestigatingAt(T26_18);
        a5.setComment("Analyst Ommen verifying — possible bonus or additional income, contacted customer for pay stub");

        // ── ALT-2606: CLOSED (TRUE_POSITIVE) — VELOCITY confirmed smurfing ──
        Alert a6 = buildAlert("ALT-2606", "High Frequency Alert (Confirmed) - ACC-102", "ACC-102", "PAY-C06", "TXN-D008",
            AlertSeverity.MEDIUM, AlertStatus.CLOSED, 78, T26_10.plus(10, ChronoUnit.MINUTES),
            "Account ACC-102 had 6 POS transactions in 10 minutes, confirmed as structuring/smurfing after investigation",
            "High Frequency Transactions", RuleType.VELOCITY, bd("6"), bd("5"), bd("4095.00"), "USD");
        a6.setAcknowledgedAt(T26_11);
        a6.setInvestigatingAt(T26_12);
        a6.setClosedAt(T26_17);
        a6.setResolutionCode(ResolutionCode.TRUE_POSITIVE);
        a6.setResolution("TRUE_POSITIVE");
        a6.setResolutionNotes("Confirmed smurfing: same account made rapid small purchases at 6 different merchants to evade single-transaction monitoring threshold, reported to compliance department and account frozen");
        a6.setVersion(3);

        // ── ALT-2607: CLOSED (FALSE_POSITIVE) — AMOUNT_THRESHOLD, recurring txn ──
        Alert a7 = buildAlert("ALT-2607", "Large Transaction Alert (False Positive) - ACC-100", "ACC-100", "PAY-A02", "TXN-D002",
            AlertSeverity.HIGH, AlertStatus.CLOSED, 76, T26_09.plus(5, ChronoUnit.MINUTES),
            "Transaction amount $2,450.00 — initially triggered alert, later confirmed below $10,000 threshold, system false positive",
            "Large Transaction Alert", RuleType.AMOUNT_THRESHOLD, bd("2450.00"), bd("10000.00"), bd("2450.00"), "USD");
        a7.setAcknowledgedAt(T26_09.plus(30, ChronoUnit.MINUTES));
        a7.setInvestigatingAt(T26_10);
        a7.setClosedAt(T26_14);
        a7.setResolutionCode(ResolutionCode.FALSE_POSITIVE);
        a7.setResolution("FALSE_POSITIVE");
        a7.setResolutionNotes("System false positive — actual transaction amount $2,450 below $10,000 threshold, threshold comparison logic corrected in new rule version");
        a7.setVersion(3);

        // ── ALT-2608: CLOSED (ESCALATED) — DAILY_LIMIT escalated ──
        Alert a8 = buildAlert("ALT-2608", "Daily Limit Alert (Escalated) - ACC-103", "ACC-103", "PAY-D03", "TXN-D011",
            AlertSeverity.HIGH, AlertStatus.CLOSED, 97, T26_13.plus(5, ChronoUnit.MINUTES),
            "Account ACC-103 daily cumulative $58,000 exceeded limit, involving offshore fund wire to Cayman Islands, suspected capital flight",
            "Daily Limit Exceeded", RuleType.DAILY_LIMIT, bd("58000.00"), bd("50000.00"), bd("58000.00"), "USD");
        a8.setAcknowledgedAt(T26_14);
        a8.setInvestigatingAt(T26_15);
        a8.setClosedAt(T27_10);
        a8.setResolutionCode(ResolutionCode.ESCALATED);
        a8.setResolution("ESCALATED");
        a8.setResolutionNotes("Escalated to law enforcement — three structured wire transfers confirmed to involve foreign exchange control evasion, Cayman account linked to PEP");
        a8.setVersion(3);

        // ── ALT-2609: DISMISSED (FALSE_POSITIVE) — NEW_PAYEE known merchant ──
        Alert a9 = buildAlert("ALT-2609", "New Payee Alert (Dismissed) - ACC-104", "ACC-104", "PAY-NEW-01", "TXN-D012",
            AlertSeverity.LOW, AlertStatus.DISMISSED, 20, T26_14.plus(10, ChronoUnit.MINUTES),
            "First transaction to Unknown Merchant Alpha — verified as affiliate of a well-known cryptocurrency exchange",
            "New Payee Transaction", RuleType.NEW_PAYEE, BigDecimal.ZERO, BigDecimal.ZERO, bd("4800.00"), "USD");
        a9.setDismissedAt(T26_15.plus(30, ChronoUnit.MINUTES));
        a9.setResolutionCode(ResolutionCode.FALSE_POSITIVE);
        a9.setResolution("FALSE_POSITIVE");
        a9.setResolutionNotes("Unknown Merchant Alpha is a registered affiliate of Binance, a known compliant exchange, no alert needed");
        a9.setVersion(1);

        // ── ALT-2610: DISMISSED (LEGITIMATE_ACTIVITY) — AMOUNT_THRESHOLD on salary ──
        Alert a10 = buildAlert("ALT-2610", "Large Deposit Alert (Dismissed) - ACC-100", "ACC-100", "PAY-A03", "TXN-D013",
            AlertSeverity.MEDIUM, AlertStatus.DISMISSED, 45, T26_16.plus(5, ChronoUnit.MINUTES),
            "ACH deposit $8,500 — confirmed by customer as quarterly performance bonus, non-suspicious funds",
            "Large Transaction Alert", RuleType.AMOUNT_THRESHOLD, bd("8500.00"), bd("10000.00"), bd("8500.00"), "USD");
        a10.setAcknowledgedAt(T26_17);
        a10.setDismissedAt(T26_18);
        a10.setResolutionCode(ResolutionCode.LEGITIMATE_ACTIVITY);
        a10.setResolution("LEGITIMATE_ACTIVITY");
        a10.setResolutionNotes("Customer provided pay stub and bonus notification, $8,500 is quarterly performance bonus, legitimate source of funds");
        a10.setVersion(2);

        // ===== 7/27 ALERTS (6) =====

        // ── ALT-2701: OPEN — AMOUNT_THRESHOLD GBP ──
        Alert a11 = buildAlert("ALT-2701", "Large Cross-border Transfer Alert - ACC-106", "ACC-106", "PAY-F01", "TXN-D014",
            AlertSeverity.HIGH, AlertStatus.OPEN, 88, T27_08.plus(5, ChronoUnit.MINUTES),
            "Account ACC-106 transferred £22,000.00 to London consulting firm, exceeds $10,000 equivalent threshold",
            "Large Transaction Alert", RuleType.AMOUNT_THRESHOLD, bd("22000.00"), bd("10000.00"), bd("22000.00"), "GBP");

        // ── ALT-2702: OPEN — NEW_PAYEE ──
        Alert a12 = buildAlert("ALT-2702", "New Payee Alert - ACC-106", "ACC-106", "PAY-F02", "TXN-D015",
            AlertSeverity.LOW, AlertStatus.OPEN, 22, T27_09.plus(3, ChronoUnit.MINUTES),
            "Account ACC-106 initiated first JPY transaction to Tokyo Suppliers KK, triggered new payee detection",
            "New Payee Transaction", RuleType.NEW_PAYEE, BigDecimal.ZERO, BigDecimal.ZERO, bd("15000.00"), "JPY");

        // ── ALT-2703: ACKNOWLEDGED — AMOUNT_THRESHOLD EUR ──
        Alert a13 = buildAlert("ALT-2703", "Large EUR Transaction Alert - ACC-106", "ACC-106", "PAY-F03", "TXN-D016",
            AlertSeverity.HIGH, AlertStatus.ACKNOWLEDGED, 90, T27_10.plus(5, ChronoUnit.MINUTES),
            "Account ACC-106 paid €35,000.00 to Paris fashion house, exceeds monitoring threshold",
            "Large Transaction Alert", RuleType.AMOUNT_THRESHOLD, bd("35000.00"), bd("10000.00"), bd("35000.00"), "EUR");
        a13.setAcknowledgedAt(T27_11);
        a13.setComment("Analyst Lele acknowledged — cross-border luxury goods purchase, requested trade contract and invoice from customer");

        // ── ALT-2704: INVESTIGATING — DAILY_LIMIT cross-border ──
        Alert a14 = buildAlert("ALT-2704", "Cross-border Daily Limit Alert - ACC-106", "ACC-106", "PAY-F01", "TXN-D014",
            AlertSeverity.HIGH, AlertStatus.INVESTIGATING, 93, T27_10.plus(10, ChronoUnit.MINUTES),
            "Account ACC-106 daily cross-border cumulative exceeds $50,000 — GBP £22,000 + EUR €35,000 sent to UK and France respectively",
            "Daily Limit Exceeded", RuleType.DAILY_LIMIT, bd("72000.00"), bd("50000.00"), bd("72000.00"), "USD");
        a14.setAcknowledgedAt(T27_10.plus(30, ChronoUnit.MINUTES));
        a14.setInvestigatingAt(T27_12);
        a14.setComment("Analyst Julie investigating — same-day large transfers to UK and France, need to examine connections and ultimate beneficial owner");

        // ── ALT-2705: DISMISSED (INSUFFICIENT_INFORMATION) — JPY misconception ──
        Alert a15 = buildAlert("ALT-2705", "JPY Transaction Alert (Dismissed) - ACC-106", "ACC-106", "PAY-F02", "TXN-D015",
            AlertSeverity.MEDIUM, AlertStatus.DISMISSED, 40, T27_09.plus(5, ChronoUnit.MINUTES),
            "JPY ¥15,000 transaction triggered alert — after currency conversion found to be only ~$100 USD, far below threshold",
            "Large Transaction Alert", RuleType.AMOUNT_THRESHOLD, bd("15000.00"), bd("10000.00"), bd("15000.00"), "JPY");
        a15.setDismissedAt(T27_09.plus(30, ChronoUnit.MINUTES));
        a15.setResolutionCode(ResolutionCode.INSUFFICIENT_INFORMATION);
        a15.setResolution("INSUFFICIENT_INFORMATION");
        a15.setResolutionNotes("System misidentified JPY 15000 as large transaction, actual ≈ $100 USD after exchange rate conversion, system needs multi-currency FX conversion logic");
        a15.setVersion(1);

        // ── ALT-2706: CLOSED (LEGITIMATE_ACTIVITY) — EUR fashion invoice verified ──
        Alert a16 = buildAlert("ALT-2706", "Large EUR Transaction (Closed) - ACC-106", "ACC-106", "PAY-F03", "TXN-D016",
            AlertSeverity.HIGH, AlertStatus.CLOSED, 85, T27_10.plus(5, ChronoUnit.MINUTES),
            "€35,000 Paris fashion purchase — customer provided trade contract and pro forma invoice, confirmed as legitimate business activity",
            "Large Transaction Alert", RuleType.AMOUNT_THRESHOLD, bd("35000.00"), bd("10000.00"), bd("35000.00"), "EUR");
        a16.setAcknowledgedAt(T27_11);
        a16.setInvestigatingAt(T27_13);
        a16.setClosedAt(T27_16);
        a16.setResolutionCode(ResolutionCode.LEGITIMATE_ACTIVITY);
        a16.setResolution("LEGITIMATE_ACTIVITY");
        a16.setResolutionNotes("Customer provided complete trade documents: purchase contract + packing list + commercial invoice, goods are 2026 Autumn/Winter fashion collection, amount consistent with market price, normal trade activity");
        a16.setVersion(3);

        // ═══════════════════════════════════════════════════════════════
        // Status history — full lifecycle chains with readable comments
        // ═══════════════════════════════════════════════════════════════

        // a1: OPEN — simple CREATED
        attachHistory(a1, AlertStatus.OPEN, null, T26_08, "[7/26 08:00] Rule engine auto-triggered: wire transfer $45,000 exceeds threshold $10,000");

        // a2: OPEN — simple CREATED
        attachHistory(a2, AlertStatus.OPEN, null, T26_10.plus(10, ChronoUnit.MINUTES), "[7/26 10:10] Rule engine auto-triggered: 6 POS transactions in 10 minutes, suspected structuring");

        // a3: ACKNOWLEDGED
        attachHistory(a3, AlertStatus.OPEN, null, T26_14, "[7/26 14:00] Rule engine auto-triggered: first transaction to Unknown Merchant Alpha");
        attachHistory(a3, AlertStatus.ACKNOWLEDGED, AlertStatus.OPEN, T26_15, "[7/26 15:00] Analyst Julie acknowledged alert — account has multiple recent new payee transactions, added to watchlist");

        // a4: INVESTIGATING (3-step chain)
        attachHistory(a4, AlertStatus.OPEN, null, T26_13.plus(5, ChronoUnit.MINUTES), "[7/26 13:05] Rule engine auto-triggered: daily cumulative $58,000 exceeded limit (real estate $25k + luxury car $18k + fund $15k)");
        attachHistory(a4, AlertStatus.ACKNOWLEDGED, AlertStatus.OPEN, T26_14, "[7/26 14:00] Analyst Lucas acknowledged — three wire amounts show declining trend, suspected structured splitting");
        attachHistory(a4, AlertStatus.INVESTIGATING, AlertStatus.ACKNOWLEDGED, T26_16, "[7/26 16:00] Escalated investigation — payee includes Cayman Islands offshore fund, need to verify ultimate beneficial owner");

        // a5: INVESTIGATING (3-step chain)
        attachHistory(a5, AlertStatus.OPEN, null, T26_16.plus(5, ChronoUnit.MINUTES), "[7/26 16:05] Rule engine triggered: ACH deposit $8,500 from Salary Inc does not match historical salary $5,200");
        attachHistory(a5, AlertStatus.ACKNOWLEDGED, AlertStatus.OPEN, T26_17, "[7/26 17:00] Analyst Ommen acknowledged — amount unusual but source is known employer, contacted customer for verification");
        attachHistory(a5, AlertStatus.INVESTIGATING, AlertStatus.ACKNOWLEDGED, T26_18, "[7/26 18:00] Awaiting customer response — sent request for pay stub and bonus proof");

        // a6: CLOSED TRUE_POSITIVE (4-step full lifecycle)
        attachHistory(a6, AlertStatus.OPEN, null, T26_10.plus(10, ChronoUnit.MINUTES), "[7/26 10:10] Rule engine triggered: ACC-102 6 POS transactions in 10 minutes");
        attachHistory(a6, AlertStatus.ACKNOWLEDGED, AlertStatus.OPEN, T26_11, "[7/26 11:00] Analyst Lucas acknowledged — 6 different merchants, evenly distributed amounts, classic structuring pattern");
        attachHistory(a6, AlertStatus.INVESTIGATING, AlertStatus.ACKNOWLEDGED, T26_12, "[7/26 12:00] Pulled surveillance footage — 6 transactions by same person at different POS terminals, confirmed smurfing");
        attachHistory(a6, AlertStatus.CLOSED, AlertStatus.INVESTIGATING, T26_17, "[7/26 17:00] Closed TRUE_POSITIVE — account frozen, case reported to compliance and FinCEN");

        // a7: CLOSED FALSE_POSITIVE (4-step full lifecycle)
        attachHistory(a7, AlertStatus.OPEN, null, T26_09.plus(5, ChronoUnit.MINUTES), "[7/26 09:05] Rule engine triggered: transaction $2,450 falsely triggered large transaction alert");
        attachHistory(a7, AlertStatus.ACKNOWLEDGED, AlertStatus.OPEN, T26_09.plus(35, ChronoUnit.MINUTES), "[7/26 09:35] Analyst Julie acknowledged — amount $2,450 clearly below $10,000 threshold, suspected rule configuration issue");
        attachHistory(a7, AlertStatus.INVESTIGATING, AlertStatus.ACKNOWLEDGED, T26_10, "[7/26 10:00] Investigated rule config — found old version rule threshold field not properly updated");
        attachHistory(a7, AlertStatus.CLOSED, AlertStatus.INVESTIGATING, T26_14, "[7/26 14:00] Closed FALSE_POSITIVE — rule threshold corrected, this transaction is normal office supply purchase");

        // a8: CLOSED ESCALATED (4-step full lifecycle)
        attachHistory(a8, AlertStatus.OPEN, null, T26_13.plus(5, ChronoUnit.MINUTES), "[7/26 13:05] Rule engine triggered: daily cumulative $58,000, all three wire transfers exceed $10,000");
        attachHistory(a8, AlertStatus.ACKNOWLEDGED, AlertStatus.OPEN, T26_14, "[7/26 14:00] Analyst Lele acknowledged high priority — Cayman Islands wire triggered PEP screening rule");
        attachHistory(a8, AlertStatus.INVESTIGATING, AlertStatus.ACKNOWLEDGED, T26_15, "[7/26 15:00] In-depth investigation — payee linked to politically exposed person, suspected capital flight and FX control evasion");
        attachHistory(a8, AlertStatus.CLOSED, AlertStatus.INVESTIGATING, T27_10, "[7/27 10:00] Closed ESCALATED — case referred to law enforcement, account frozen, 3 transactions fully recovered");

        // a9: DISMISSED FALSE_POSITIVE
        attachHistory(a9, AlertStatus.OPEN, null, T26_14.plus(10, ChronoUnit.MINUTES), "[7/26 14:10] Rule engine triggered: new payee Unknown Merchant Alpha");
        attachHistory(a9, AlertStatus.DISMISSED, AlertStatus.OPEN, T26_15.plus(30, ChronoUnit.MINUTES), "[7/26 15:30] Dismissed FALSE_POSITIVE — verified as Binance affiliate, added to whitelist");

        // a10: DISMISSED LEGITIMATE_ACTIVITY (with ACKNOWLEDGED)
        attachHistory(a10, AlertStatus.OPEN, null, T26_16.plus(5, ChronoUnit.MINUTES), "[7/26 16:05] Rule engine triggered: ACH deposit $8,500");
        attachHistory(a10, AlertStatus.ACKNOWLEDGED, AlertStatus.OPEN, T26_17, "[7/26 17:00] Analyst Julie acknowledged — customer responded, provided pay stub and bonus notification");
        attachHistory(a10, AlertStatus.DISMISSED, AlertStatus.ACKNOWLEDGED, T26_18, "[7/26 18:00] Dismissed LEGITIMATE_ACTIVITY — $8,500 is quarterly performance bonus, legitimate source of funds");

        // a11: OPEN (7/27)
        attachHistory(a11, AlertStatus.OPEN, null, T27_08.plus(5, ChronoUnit.MINUTES), "[7/27 08:05] Rule engine triggered: London transfer £22,000, equivalent ~$28,000 exceeds threshold");

        // a12: OPEN (7/27)
        attachHistory(a12, AlertStatus.OPEN, null, T27_09.plus(3, ChronoUnit.MINUTES), "[7/27 09:03] Rule engine triggered: first JPY transaction to Tokyo Suppliers KK");

        // a13: ACKNOWLEDGED (7/27)
        attachHistory(a13, AlertStatus.OPEN, null, T27_10.plus(5, ChronoUnit.MINUTES), "[7/27 10:05] Rule engine triggered: Paris payment €35,000, cross-border large luxury purchase");
        attachHistory(a13, AlertStatus.ACKNOWLEDGED, AlertStatus.OPEN, T27_11, "[7/27 11:00] Analyst Lele acknowledged — requested trade contract and commercial invoice from customer");

        // a14: INVESTIGATING (7/27, 3-step)
        attachHistory(a14, AlertStatus.OPEN, null, T27_10.plus(10, ChronoUnit.MINUTES), "[7/27 10:10] Rule engine triggered: daily cross-border cumulative exceeds $50,000 (GBP £22k + EUR €35k)");
        attachHistory(a14, AlertStatus.ACKNOWLEDGED, AlertStatus.OPEN, T27_10.plus(30, ChronoUnit.MINUTES), "[7/27 10:30] Analyst Julie acknowledged — same-day transfers to UK and France, need to investigate connections");
        attachHistory(a14, AlertStatus.INVESTIGATING, AlertStatus.ACKNOWLEDGED, T27_12, "[7/27 12:00] In-depth investigation — verifying whether the two payees share a common beneficiary");

        // a15: DISMISSED INSUFFICIENT_INFORMATION (7/27)
        attachHistory(a15, AlertStatus.OPEN, null, T27_09.plus(5, ChronoUnit.MINUTES), "[7/27 09:05] Rule engine triggered: JPY ¥15,000 identified as large transaction");
        attachHistory(a15, AlertStatus.DISMISSED, AlertStatus.OPEN, T27_09.plus(30, ChronoUnit.MINUTES), "[7/27 09:30] Dismissed INSUFFICIENT_INFORMATION — ¥15,000 ≈ $100 USD, system lacks multi-currency FX conversion");

        // a16: CLOSED LEGITIMATE_ACTIVITY (7/27, 4-step full lifecycle)
        attachHistory(a16, AlertStatus.OPEN, null, T27_10.plus(5, ChronoUnit.MINUTES), "[7/27 10:05] Rule engine triggered: €35,000 Paris fashion purchase triggered large transaction alert");
        attachHistory(a16, AlertStatus.ACKNOWLEDGED, AlertStatus.OPEN, T27_11, "[7/27 11:00] Analyst Lele acknowledged — customer submitted purchase contract and pro forma invoice");
        attachHistory(a16, AlertStatus.INVESTIGATING, AlertStatus.ACKNOWLEDGED, T27_13, "[7/27 13:00] Document review — contract amount €35,000 matches remittance, packing list matches commercial invoice");
        attachHistory(a16, AlertStatus.CLOSED, AlertStatus.INVESTIGATING, T27_16, "[7/27 16:00] Closed LEGITIMATE_ACTIVITY — goods are 2026 Autumn/Winter fashion collection, amount consistent with market, normal trade");

        // ═══════════════════════════════════════════════════════════════
        // Alert ↔ Transaction associations
        // ═══════════════════════════════════════════════════════════════

        // a1: OPEN AMOUNT_THRESHOLD → t1 (ACC-101, 45k WIRE)
        attachTx(a1, t1, true);

        // a2: OPEN VELOCITY → t3-t8 (ACC-102 velocity batch)
        attachTx(a2, t3, true);
        attachTx(a2, t4, false);
        attachTx(a2, t5, false);
        attachTx(a2, t6, false);
        attachTx(a2, t7, false);
        attachTx(a2, t8, false);

        // a3: ACKNOWLEDGED NEW_PAYEE → t12
        attachTx(a3, t12, true);

        // a4: INVESTIGATING DAILY_LIMIT → t9,t10,t11 (ACC-103)
        attachTx(a4, t9, true);
        attachTx(a4, t10, false);
        attachTx(a4, t11, false);

        // a5: INVESTIGATING AMOUNT_THRESHOLD → t13 (CREDIT)
        attachTx(a5, t13, true);

        // a6: CLOSED TRUE_POSITIVE → t3-t8 (ACC-102 velocity)
        attachTx(a6, t3, true);
        attachTx(a6, t4, false);
        attachTx(a6, t5, false);

        // a7: CLOSED FALSE_POSITIVE → t2
        attachTx(a7, t2, true);

        // a8: CLOSED ESCALATED → t9,t10,t11 (ACC-103)
        attachTx(a8, t9, true);
        attachTx(a8, t10, false);
        attachTx(a8, t11, false);

        // a9: DISMISSED FALSE_POSITIVE → t12
        attachTx(a9, t12, true);

        // a10: DISMISSED LEGITIMATE → t13
        attachTx(a10, t13, true);

        // a11: OPEN AMOUNT_THRESHOLD GBP → t14
        attachTx(a11, t14, true);

        // a12: OPEN NEW_PAYEE JPY → t15
        attachTx(a12, t15, true);

        // a13: ACKNOWLEDGED AMOUNT_THRESHOLD EUR → t16
        attachTx(a13, t16, true);

        // a14: INVESTIGATING DAILY_LIMIT → t14, t16
        attachTx(a14, t14, true);
        attachTx(a14, t16, false);

        // a15: DISMISSED INSUFFICIENT → t15
        attachTx(a15, t15, true);

        // a16: CLOSED LEGITIMATE → t16
        attachTx(a16, t16, true);

        alertRepository.saveAll(List.of(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10,
            a11, a12, a13, a14, a15, a16));

        log.info("Sample monitoring data initialized: {} transactions, {} alerts.",
            transactionRepository.count(), alertRepository.count());
    }

    // ═══════════════════════════════════════════════════════════════
    // Helper methods
    // ═══════════════════════════════════════════════════════════════

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    private Transaction buildTx(String txId, String accountId, String payeeId, String payeeName,
                                 TransactionType type, BigDecimal amount, String currency,
                                 TransactionStatus status, Instant txTime,
                                 String channel, String country, String description) {
        Transaction tx = new Transaction();
        tx.setTransactionId(txId);
        tx.setAccountId(accountId);
        tx.setPayeeId(payeeId);
        tx.setPayeeName(payeeName);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setStatus(status);
        tx.setTransactionTime(txTime);
        tx.setReceivedAt(txTime.plus(2, ChronoUnit.MINUTES));
        tx.setEvaluatedAt(status == TransactionStatus.COMPLETED ? txTime.plus(3, ChronoUnit.MINUTES) : null);
        tx.setEvaluationMode(status == TransactionStatus.COMPLETED ? "REAL_TIME" : null);
        tx.setPaymentChannel(channel);
        tx.setCountry(country);
        tx.setDescription(description);
        tx.setVersion(1);
        tx.setLateArrival(false);
        return tx;
    }

    private Alert buildAlert(String alertId, String title, String accountId, String payeeId, String txId,
                               AlertSeverity severity, AlertStatus status, Integer riskScore,
                               Instant createdAt, String triggerReason, String ruleName,
                               RuleType ruleType, BigDecimal actualValue, BigDecimal thresholdValue,
                               BigDecimal totalAmount, String currency) {
        Alert a = new Alert();
        a.setAlertId(alertId);
        a.setTitle(title);
        a.setDescription(triggerReason);
        a.setAccountId(accountId);
        a.setPrimaryPayeeId(payeeId);
        a.setPrimaryTransactionId(txId);
        a.setStatus(status);
        a.setSeverity(severity);
        a.setRiskScore(riskScore);
        a.setCreatedAt(createdAt);
        a.setUpdatedAt(createdAt.plus(5, ChronoUnit.MINUTES));
        a.setTotalAmount(totalAmount);
        a.setCurrency(currency);
        a.setTransactionCount(1);
        a.setFirstTransactionAt(createdAt.minus(30, ChronoUnit.MINUTES));
        a.setLastTransactionAt(createdAt);
        a.setRuleId("1");
        a.setRuleName(ruleName);
        a.setRuleType(ruleType.name());
        a.setTriggerReason(triggerReason);
        a.setActualValue(actualValue);
        a.setThresholdValue(thresholdValue);
        a.setVersion(1);
        return a;
    }

    private void attachHistory(Alert alert, AlertStatus toStatus, AlertStatus fromStatus,
                                 Instant changedAt, String comment) {
        AlertHistory h = new AlertHistory();
        h.setAlert(alert);
        h.setActionType(toStatus == AlertStatus.OPEN ? "CREATED" : "STATUS_CHANGED");
        h.setFromStatus(fromStatus);
        h.setToStatus(toStatus);
        h.setChangedAt(changedAt);
        h.setComment(comment);
        alert.getStatusHistory().add(h);
    }

    private void attachTx(Alert alert, Transaction tx, boolean primary) {
        AlertTransaction at = new AlertTransaction();
        at.setAlert(alert);
        at.setTransactionId(tx.getTransactionId());
        at.setPrimaryTrigger(primary);
        alert.getAlertTransactions().add(at);
    }
}