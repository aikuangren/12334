package com.fiftytalk.carddraw.dto;

import com.fiftytalk.carddraw.domain.Rarity;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminSlotRuleUpdateRequest(
        @NotEmpty List<Item> rules
) {
    public record Item(
            @NotNull Integer slotNo,
            @NotNull Rarity rarity,
            Double probabilityPercent,
            Integer probabilityBasisPoints
    ) {
    }
}
