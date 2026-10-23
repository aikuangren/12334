package com.fiftytalk.carddraw.dto;

import com.fiftytalk.carddraw.domain.PoolStatus;
import jakarta.validation.constraints.NotBlank;

public record AdminPoolRequest(
        @NotBlank String version,
        @NotBlank String name,
        String packImageUrl,
        Integer displaySortNo,
        PoolStatus status,
        Long copyRulesFromPoolId
) {
}
