package com.fiftytalk.carddraw.dto;

import com.fiftytalk.carddraw.domain.CardStatus;
import com.fiftytalk.carddraw.domain.Rarity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminCardRequest(
        @NotBlank String word,
        String meaning,
        String arabicMeaning,
        String audioUrl,
        String imageUrl,
        @NotNull Rarity rarity,
        @NotBlank String poolVersion,
        Integer sortNo,
        CardStatus status
) {
}
