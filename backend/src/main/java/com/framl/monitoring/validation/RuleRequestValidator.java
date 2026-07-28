package com.framl.monitoring.validation;

import com.framl.monitoring.dto.RuleRequest;
import com.framl.monitoring.enums.RuleType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RuleRequestValidator implements ConstraintValidator<ValidRuleRequest, RuleRequest> {

    @Override
    public boolean isValid(RuleRequest value, ConstraintValidatorContext context) {
        if (value == null || value.getRuleType() == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        boolean valid = true;

        RuleType ruleType = value.getRuleType();
        switch (ruleType) {
            case AMOUNT_THRESHOLD -> {
                if (value.getThreshold() == null) {
                    addViolation(context, "threshold", "threshold is required for AMOUNT_THRESHOLD");
                    valid = false;
                }
            }
            case VELOCITY -> {
                if (value.getMaxTransactionCount() == null) {
                    addViolation(context, "maxTransactionCount", "maxTransactionCount is required for VELOCITY");
                    valid = false;
                }
                if (value.getTimeWindowMinutes() == null) {
                    addViolation(context, "timeWindowMinutes", "timeWindowMinutes is required for VELOCITY");
                    valid = false;
                }
            }
            case DAILY_LIMIT -> {
                if (value.getDailyLimit() == null) {
                    addViolation(context, "dailyLimit", "dailyLimit is required for DAILY_LIMIT");
                    valid = false;
                }
                if (value.getCurrency() == null || value.getCurrency().isBlank()) {
                    addViolation(context, "currency", "currency is required for DAILY_LIMIT");
                    valid = false;
                }
            }
            case NEW_PAYEE -> {
                // No rule-specific required fields for NEW_PAYEE.
            }
        }

        return valid;
    }

    private void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}