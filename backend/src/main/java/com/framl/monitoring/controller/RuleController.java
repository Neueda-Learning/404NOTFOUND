package com.framl.monitoring.controller;

import com.framl.monitoring.dto.RuleRequest;
import com.framl.monitoring.dto.RuleResponse;
import com.framl.monitoring.service.RuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @GetMapping
    public List<RuleResponse> listAll() {
        return ruleService.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RuleResponse> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ruleService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<RuleResponse> create(@Valid @RequestBody RuleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RuleResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody RuleRequest req) {
        try {
            return ResponseEntity.ok(ruleService.update(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<RuleResponse> toggle(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ruleService.toggle(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ruleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
