package com.framl.monitoring.repository;

import com.framl.monitoring.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleRepository extends JpaRepository<Rule, Long> {
    List<Rule> findByActiveTrue();
}
