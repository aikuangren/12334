package com.fiftytalk.carddraw.repository;

import com.fiftytalk.carddraw.entity.DrawSlotRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrawSlotRuleRepository extends JpaRepository<DrawSlotRule, Long> {
    List<DrawSlotRule> findByPoolIdOrderBySlotNoAscIdAsc(Long poolId);
    List<DrawSlotRule> findByPoolIdAndSlotNo(Long poolId, Integer slotNo);
}

