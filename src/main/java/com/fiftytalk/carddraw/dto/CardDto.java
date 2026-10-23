package com.fiftytalk.carddraw.dto;

import com.fiftytalk.carddraw.domain.Rarity;
import com.fiftytalk.carddraw.entity.Card;

public record CardDto(
        Long cardId,
        String word,
        String meaning,
        String arabicMeaning,
        String audioUrl,
        String imageUrl,
        Rarity rarity,
        Integer ownedCount,
        Boolean isNew
) {
    public static CardDto from(Card card, Integer ownedCount, Boolean isNew) {
        return new CardDto(
                card.getId(),
                card.getWord(),
                card.getMeaning(),
                card.getArabicMeaning(),
                card.getAudioUrl(),
                card.getImageUrl(),
                card.getRarity(),
                ownedCount,
                isNew
        );
    }
}
