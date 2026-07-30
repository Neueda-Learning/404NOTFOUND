package com.framl.monitoring.service;

import com.framl.monitoring.dto.RuleRequest;
import com.framl.monitoring.dto.RuleResponse;
import com.framl.monitoring.entity.Rule;
import com.framl.monitoring.enums.AlertSeverity;
import com.framl.monitoring.enums.RuleType;
import com.framl.monitoring.repository.AlertRepository;
import com.framl.monitoring.repository.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private AlertRepository alertRepository;

    private RuleService ruleService;

    @BeforeEach
    void setUp() {
        ruleService = new RuleService(ruleRepository, alertRepository);
    }

    @Test
    void create_appliesRequestAndDefaultsActiveTrue() {
        RuleRequest req = buildRequest();
        req.setActive(null);

        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> {
            Rule r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        RuleResponse response = ruleService.create(req);

        assertEquals(1L, response.getId());
        assertEquals("High Frequency", response.getName());
        assertEquals(RuleType.VELOCITY, response.getRuleType());
        assertEquals(AlertSeverity.MEDIUM, response.getSeverity());
        assertTrue(response.getActive());
        assertEquals(5, response.getMaxTransactionCount());
        assertEquals(10, response.getTimeWindowMinutes());
    }

    @Test
    void update_changesExistingRuleFields() {
        Rule existing = new Rule();
        existing.setId(10L);
        existing.setName("Old Name");
        existing.setActive(true);

        RuleRequest req = buildRequest();
        req.setName("Updated Name");
        req.setThreshold(new BigDecimal("12000"));

        when(ruleRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = ruleService.update(10L, req);

        assertEquals("Updated Name", response.getName());
        assertEquals(new BigDecimal("12000"), response.getThreshold());
        assertEquals("USD", response.getCurrency());
    }

    @Test
    void toggle_invertsActiveFlag() {
        Rule existing = new Rule();
        existing.setId(2L);
        existing.setActive(false);

        when(ruleRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = ruleService.toggle(2L);

        assertTrue(response.getActive());
    }

    @Test
    void listAll_andGetById_mapToResponses() {
        Rule one = new Rule();
        one.setId(1L);
        one.setName("R1");
        one.setRuleType(RuleType.AMOUNT_THRESHOLD);
        one.setSeverity(AlertSeverity.HIGH);
        one.setActive(true);

        Rule two = new Rule();
        two.setId(2L);
        two.setName("R2");
        two.setRuleType(RuleType.NEW_PAYEE);
        two.setSeverity(AlertSeverity.LOW);
        two.setActive(false);

        when(ruleRepository.findAll()).thenReturn(List.of(one, two));
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(one));

        List<RuleResponse> all = ruleService.listAll();
        RuleResponse byId = ruleService.getById(1L);

        assertEquals(2, all.size());
        assertEquals("R1", byId.getName());
        assertEquals(RuleType.AMOUNT_THRESHOLD, byId.getRuleType());
    }

    @Test
    void delete_delegatesToRepository() {
        ruleService.delete(7L);
        verify(ruleRepository).deleteById(7L);
    }

    @Test
    void getById_throwsWhenMissing() {
        when(ruleRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ruleService.getById(99L));
        assertEquals("Rule not found: 99", ex.getMessage());
    }

    private RuleRequest buildRequest() {
        RuleRequest req = new RuleRequest();
        req.setName("High Frequency");
        req.setDescription("Alert on rapid transactions");
        req.setRuleType(RuleType.VELOCITY);
        req.setSeverity(AlertSeverity.MEDIUM);
        req.setActive(true);
        req.setThreshold(new BigDecimal("10000"));
        req.setMaxTransactionCount(5);
        req.setTimeWindowMinutes(10);
        req.setDailyLimit(new BigDecimal("30000"));
        req.setCurrency("USD");
        req.setTransactionTypes(List.of("DEBIT", "TRANSFER"));
        return req;
    }
}
