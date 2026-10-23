package com.fiftytalk.carddraw.repository;

import com.fiftytalk.carddraw.entity.NewCardProtectionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewCardProtectionRuleRepository extends JpaRepository<NewCardProtectionRule, Long> {
    List<NewCardProtectionRule> findAllByOrderByMinCollectionRateAsc();
}

