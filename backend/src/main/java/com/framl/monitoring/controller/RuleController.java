package com.framl.monitoring.controller;

import com.framl.monitoring.dto.RuleRequest;
import com.framl.monitoring.dto.RuleResponse;
import com.framl.monitoring.service.RuleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
@Validated
public class RuleController {

    private final RuleService ruleService;

    @GetMapping
    public List<RuleResponse> listAll() {
        return ruleService.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RuleResponse> getById(@PathVariable @NotNull @Positive Long id) {
        return ResponseEntity.ok(ruleService.getById(id));
    }

    @PostMapping
    public ResponseEntity<RuleResponse> create(@Valid @RequestBody RuleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RuleResponse> update(@PathVariable @NotNull @Positive Long id,
                                                @Valid @RequestBody RuleRequest req) {
        return ResponseEntity.ok(ruleService.update(id, req));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<RuleResponse> toggle(@PathVariable @NotNull @Positive Long id) {
        return ResponseEntity.ok(ruleService.toggle(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NotNull @Positive Long id) {
        ruleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
