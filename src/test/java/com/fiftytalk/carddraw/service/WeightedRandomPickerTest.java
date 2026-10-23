package com.fiftytalk.carddraw.service;

import com.fiftytalk.carddraw.domain.Rarity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WeightedRandomPickerTest {
    @Test
    void shouldPickOnlyPositiveWeightedItemWhenOthersAreZero() {
        String picked = WeightedRandomPicker.pick(List.of("A", "B"), item -> item.equals("A") ? 0 : 10);
        assertThat(picked).isEqualTo("B");
    }

    @Test
    void shouldApplySecondDrawAndSixDrawUrPlusPolicies() {
        assertThat(DrawGuaranteePolicy.guaranteedRarityForDraw(1)).isEmpty();
        assertThat(DrawGuaranteePolicy.guaranteedRarityForDraw(2)).contains(Rarity.SSR);
        assertThat(DrawGuaranteePolicy.guaranteedRarityForDraw(6)).isEmpty();
        assertThat(DrawGuaranteePolicy.allowedRaritiesForSlot(6, DrawGuaranteePolicy.GUARANTEED_RARITY_SLOT_NO))
                .isEqualTo(Set.of(Rarity.UR, Rarity.LR, Rarity.XR));
    }

    @Test
    void shouldCalculateDrawsUntilNextUrPlusGuaranteeWithinOneToSix() {
        assertThat(DrawGuaranteePolicy.drawsUntilNextUrPlus(0)).isEqualTo(6);
        assertThat(DrawGuaranteePolicy.drawsUntilNextUrPlus(1)).isEqualTo(5);
        assertThat(DrawGuaranteePolicy.drawsUntilNextUrPlus(5)).isEqualTo(1);
        assertThat(DrawGuaranteePolicy.drawsUntilNextUrPlus(6)).isEqualTo(6);
    }
}
