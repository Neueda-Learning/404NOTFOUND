package com.framl.monitoring.config;

import com.framl.monitoring.entity.Rule;
import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.RuleType;
import com.framl.monitoring.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RuleRepository ruleRepository;

    @Override
    public void run(String... args) {
        if (ruleRepository.count() == 0) {
            log.info("Initializing default monitoring rules...");
            initRules();
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
}
