package com.fiftytalk.carddraw.config;

import com.fiftytalk.carddraw.domain.Rarity;
import com.fiftytalk.carddraw.entity.DrawSlotRule;

import java.util.List;

public final class DefaultDrawSlotRules {
    private static final List<Template> TEMPLATES = List.of(
            new Template(1, Rarity.R, 9500),
            new Template(1, Rarity.SR, 500),
            new Template(2, Rarity.R, 9500),
            new Template(2, Rarity.SR, 500),
            new Template(3, Rarity.R, 9500),
            new Template(3, Rarity.SR, 500),
            new Template(4, Rarity.R, 4000),
            new Template(4, Rarity.SR, 5800),
            new Template(4, Rarity.SSR, 200),
            new Template(5, Rarity.SR, 8000),
            new Template(5, Rarity.SSR, 1900),
            new Template(5, Rarity.UR, 50),
            new Template(5, Rarity.LR, 30),
            new Template(5, Rarity.XR, 20)
    );

    private DefaultDrawSlotRules() {
    }

    public static List<DrawSlotRule> forPool(Long poolId) {
        return TEMPLATES.stream()
                .map(template -> new DrawSlotRule(poolId, template.slotNo(), template.rarity(), template.probabilityBasisPoints()))
                .toList();
    }

    private record Template(int slotNo, Rarity rarity, int probabilityBasisPoints) {
    }
}
