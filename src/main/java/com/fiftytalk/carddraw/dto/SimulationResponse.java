package com.fiftytalk.carddraw.dto;

import java.util.List;
import java.util.Map;

public record SimulationResponse(
        Integer packs,
        Integer totalCards,
        Integer uniqueCardsObtained,
        Integer totalCardsInPool,
        Map<String, Integer> rarityCounts,
        Map<String, Double> rarityRates,
        Map<Integer, Map<String, Integer>> slotRarityCounts,
        RuleCheck ruleCheck,
        List<CollectionProgressPoint> collectionProgress,
        String conclusion
) {
    public record RuleCheck(
            Integer invalidPackCount,
            Integer slot4InvalidCount,
            Integer slot5InvalidCount,
            Integer srPlusGuaranteeInvalidCount,
            Integer duplicateGroupInvalidCount,
            Integer sameCardMoreThanTwoInvalidCount,
            Integer slot45DuplicateInvalidCount
    ) {
    }

    public record CollectionProgressPoint(
            Integer packNo,
            Integer obtained,
            Integer total,
            Integer rate
    ) {
    }
}

