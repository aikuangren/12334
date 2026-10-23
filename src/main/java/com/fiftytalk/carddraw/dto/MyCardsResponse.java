package com.fiftytalk.carddraw.dto;

import java.util.List;
import java.util.Map;

public record MyCardsResponse(
        Long userId,
        Integer obtainedCardCount,
        Integer totalCardCount,
        Map<String, RarityProgress> rarityProgress,
        List<CardDto> cards
) {
    public record RarityProgress(Integer obtained, Integer total, Integer rate) {
    }
}

