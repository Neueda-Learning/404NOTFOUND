package com.framl.monitoring.service;

import com.framl.monitoring.dto.RuleRequest;
import com.framl.monitoring.dto.RuleResponse;
import com.framl.monitoring.entity.Rule;
import com.framl.monitoring.repository.AlertRepository;
import com.framl.monitoring.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;
    private final AlertRepository alertRepository;

    public List<RuleResponse> listAll() {
        return ruleRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public RuleResponse getById(Long id) {
        return toResponse(findRule(id));
    }

    @Transactional
    public RuleResponse create(RuleRequest req) {
        Rule rule = new Rule();
        applyRequest(rule, req);
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional
    public RuleResponse update(Long id, RuleRequest req) {
        Rule rule = findRule(id);
        applyRequest(rule, req);
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional
    public RuleResponse toggle(Long id) {
        Rule rule = findRule(id);
        rule.setActive(!rule.getActive());
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional
    public void delete(Long id) {
        ruleRepository.deleteById(id);
    }

    private void applyRequest(Rule rule, RuleRequest req) {
        rule.setName(req.getName());
        rule.setDescription(req.getDescription());
        rule.setRuleType(req.getRuleType());
        rule.setSeverity(req.getSeverity());
        rule.setActive(req.getActive() != null ? req.getActive() : true);
        rule.setThreshold(req.getThreshold());
        rule.setMaxTransactionCount(req.getMaxTransactionCount());
        rule.setTimeWindowMinutes(req.getTimeWindowMinutes());
        rule.setDailyLimit(req.getDailyLimit());
        rule.setCurrency(req.getCurrency());
        rule.setTransactionTypes(req.getTransactionTypes());
    }

    private Rule findRule(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));
    }

    private RuleResponse toResponse(Rule r) {
        RuleResponse resp = new RuleResponse();
        resp.setId(r.getId());
        resp.setName(r.getName());
        resp.setDescription(r.getDescription());
        resp.setRuleType(r.getRuleType());
        resp.setSeverity(r.getSeverity());
        resp.setActive(r.getActive());
        resp.setThreshold(r.getThreshold());
        resp.setMaxTransactionCount(r.getMaxTransactionCount());
        resp.setTimeWindowMinutes(r.getTimeWindowMinutes());
        resp.setDailyLimit(r.getDailyLimit());
        resp.setCurrency(r.getCurrency());
        resp.setTransactionTypes(r.getTransactionTypes());
        resp.setVersion(r.getVersion());
        resp.setCreatedAt(r.getCreatedAt());
        resp.setUpdatedAt(r.getUpdatedAt());
        return resp;
    }
}
