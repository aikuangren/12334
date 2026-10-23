package com.fiftytalk.carddraw.config;

import com.fiftytalk.carddraw.domain.CardStatus;
import com.fiftytalk.carddraw.domain.PoolStatus;
import com.fiftytalk.carddraw.domain.Rarity;

import java.util.List;
import java.util.Map;

public record StaticCardPoolConfig(
        Integer schemaVersion,
        Pool pool,
        Quota quota,
        Assets assets,
        List<Rarity> rarities,
        Map<Rarity, Integer> rarityCounts,
        List<SlotRule> slotRules,
        List<NewCardProtectionRule> newCardProtectionRules,
        NewCardProtectionStrategy newCardProtectionStrategy,
        List<Card> cards
) {
    public record Pool(
            String version,
            String name,
            PoolStatus status,
            String packImageUrl,
            Integer displaySortNo
    ) {
    }

    public record Quota(
            Integer dailyFreeQuota,
            Integer businessResetHour,
            Boolean simulateNextDayEnabled,
            Boolean courseResetEnabled,
            Boolean practiceExtraEnabled
    ) {
    }

    public record Assets(
            String baseUrl,
            String h5Path,
            String cardImagePath,
            String audioPath,
            String packPath,
            String videoPath
    ) {
    }

    public record SlotRule(
            Integer slotNo,
            Rarity rarity,
            Integer probabilityBasisPoints
    ) {
    }

    public record NewCardProtectionRule(
            Integer minCollectionRate,
            Integer maxCollectionRate,
            Double newCardWeightMultiplier
    ) {
    }

    public record NewCardProtectionStrategy(
            CollectionRateBand consecutiveNoNewProtection,
            CollectionRateBand dailyNewCardProtection
    ) {
    }

    public record CollectionRateBand(
            Integer minCollectionRate,
            Integer maxCollectionRate
    ) {
    }

    public record Card(
            Long cardId,
            String word,
            String meaning,
            String arabicMeaning,
            Rarity rarity,
            Integer sortNo,
            String category,
            String source,
            String audioUrl,
            String imageUrl,
            CardStatus status
    ) {
    }
}
