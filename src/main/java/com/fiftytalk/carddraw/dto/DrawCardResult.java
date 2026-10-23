package com.fiftytalk.carddraw.dto;

import com.fiftytalk.carddraw.domain.Rarity;

public record DrawCardResult(
        Integer slotNo,
        Long cardId,
        String word,
        String meaning,
        String arabicMeaning,
        String audioUrl,
        String imageUrl,
        Rarity rarity,
        Boolean isNew,
        Integer ownedCount
) {
}
