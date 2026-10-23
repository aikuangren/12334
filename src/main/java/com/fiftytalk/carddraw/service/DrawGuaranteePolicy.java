package com.fiftytalk.carddraw.service;

import com.fiftytalk.carddraw.domain.Rarity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

final class DrawGuaranteePolicy {
    static final int GUARANTEED_RARITY_SLOT_NO = 5;
    private static final int SECOND_DRAW_NO = 2;
    static final int UR_PLUS_GUARANTEE_CYCLE = 6;
    private static final Set<Rarity> UR_PLUS_RARITIES =
            Collections.unmodifiableSet(EnumSet.of(Rarity.UR, Rarity.LR, Rarity.XR));

    private DrawGuaranteePolicy() {
    }

    static Optional<Rarity> guaranteedRarityForDraw(int drawNo) {
        if (drawNo == SECOND_DRAW_NO) {
            return Optional.of(Rarity.SSR);
        }
        return Optional.empty();
    }

    static Optional<Rarity> fixedRarityForSlot(int drawNo, int slotNo) {
        return slotNo == GUARANTEED_RARITY_SLOT_NO
                ? guaranteedRarityForDraw(drawNo)
                : Optional.empty();
    }

    static Set<Rarity> allowedRaritiesForSlot(int drawNo, int slotNo) {
        if (slotNo == GUARANTEED_RARITY_SLOT_NO && isUrPlusGuaranteeDraw(drawNo)) {
            return UR_PLUS_RARITIES;
        }
        return Collections.emptySet();
    }

    static boolean requiresHighRarity(int drawNo, int slotNo) {
        return !allowedRaritiesForSlot(drawNo, slotNo).isEmpty();
    }

    static boolean isUrPlusGuaranteeDraw(int drawNo) {
        return drawNo > 0 && drawNo % UR_PLUS_GUARANTEE_CYCLE == 0;
    }

    static int drawsUntilNextUrPlus(long completedDraws) {
        return UR_PLUS_GUARANTEE_CYCLE
                - (int) Math.floorMod(completedDraws, UR_PLUS_GUARANTEE_CYCLE);
    }
}
