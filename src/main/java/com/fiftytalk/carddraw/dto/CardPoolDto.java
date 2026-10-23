package com.fiftytalk.carddraw.dto;

import com.fiftytalk.carddraw.entity.CardPool;

public record CardPoolDto(
        Long id,
        String version,
        String name,
        String packImageUrl,
        Integer displaySortNo
) {
    public static CardPoolDto from(CardPool pool) {
        return new CardPoolDto(
                pool.getId(),
                pool.getVersion(),
                pool.getName(),
                pool.getPackImageUrl(),
                pool.getDisplaySortNo()
        );
    }
}
