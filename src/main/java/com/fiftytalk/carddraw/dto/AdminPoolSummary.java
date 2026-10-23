package com.fiftytalk.carddraw.dto;

import com.fiftytalk.carddraw.domain.PoolStatus;
import com.fiftytalk.carddraw.entity.CardPool;

public record AdminPoolSummary(
        Long id,
        String version,
        String name,
        String packImageUrl,
        Integer displaySortNo,
        PoolStatus status,
        long cardCount,
        boolean active
) {
    public static AdminPoolSummary from(CardPool pool, long cardCount, boolean active) {
        return new AdminPoolSummary(
                pool.getId(),
                pool.getVersion(),
                pool.getName(),
                pool.getPackImageUrl(),
                pool.getDisplaySortNo(),
                pool.getStatus(),
                cardCount,
                active
        );
    }
}
