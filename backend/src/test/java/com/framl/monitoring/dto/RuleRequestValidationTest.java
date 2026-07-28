package com.framl.monitoring.dto;

import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.RuleType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void amountThreshold_requiresThreshold() {
        RuleRequest request = baseRequest(RuleType.AMOUNT_THRESHOLD);
        request.setThreshold(null);

        Set<ConstraintViolation<RuleRequest>> violations = validator.validate(request);

        assertHasFieldViolation(violations, "threshold", "threshold is required for AMOUNT_THRESHOLD");
    }

    @Test
    void velocity_requiresCountAndWindow() {
        RuleRequest request = baseRequest(RuleType.VELOCITY);
        request.setMaxTransactionCount(null);
        request.setTimeWindowMinutes(null);

        Set<ConstraintViolation<RuleRequest>> violations = validator.validate(request);

        assertHasFieldViolation(violations, "maxTransactionCount", "maxTransactionCount is required for VELOCITY");
        assertHasFieldViolation(violations, "timeWindowMinutes", "timeWindowMinutes is required for VELOCITY");
    }

    @Test
    void dailyLimit_requiresDailyLimitAndCurrency() {
        RuleRequest request = baseRequest(RuleType.DAILY_LIMIT);
        request.setDailyLimit(null);
        request.setCurrency(null);

        Set<ConstraintViolation<RuleRequest>> violations = validator.validate(request);

        assertHasFieldViolation(violations, "dailyLimit", "dailyLimit is required for DAILY_LIMIT");
        assertHasFieldViolation(violations, "currency", "currency is required for DAILY_LIMIT");
    }

    @Test
    void newPayee_doesNotRequireThresholdWindowOrDailyLimit() {
        RuleRequest request = baseRequest(RuleType.NEW_PAYEE);
        request.setThreshold(null);
        request.setMaxTransactionCount(null);
        request.setTimeWindowMinutes(null);
        request.setDailyLimit(null);
        request.setCurrency(null);

        Set<ConstraintViolation<RuleRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    private RuleRequest baseRequest(RuleType type) {
        RuleRequest request = new RuleRequest();
        request.setName("Rule Name");
        request.setRuleType(type);
        request.setSeverity(AlertSeverity.HIGH);
        request.setThreshold(new BigDecimal("1000"));
        request.setMaxTransactionCount(5);
        request.setTimeWindowMinutes(10);
        request.setDailyLimit(new BigDecimal("20000"));
        request.setCurrency("USD");
        return request;
    }

    private void assertHasFieldViolation(Set<ConstraintViolation<RuleRequest>> violations,
                                         String field,
                                         String message) {
        assertTrue(violations.stream().anyMatch(v ->
                field.equals(v.getPropertyPath().toString()) && message.equals(v.getMessage())));
    }
}