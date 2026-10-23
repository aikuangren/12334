package com.fiftytalk.carddraw.dto;

import com.fiftytalk.carddraw.entity.UserDrawQuota;

import java.time.LocalDate;
import java.time.Instant;

public record QuotaDto(
        Integer freeQuota,
        Integer extraQuota,
        Integer availableQuota,
        Integer usedToday,
        LocalDate quotaDate,
        Instant nextResetAt
) {
    public static QuotaDto from(UserDrawQuota quota, Instant nextResetAt) {
        return new QuotaDto(
                quota.getFreeQuota(),
                quota.getExtraQuota(),
                quota.available(),
                quota.getUsedToday(),
                quota.getQuotaDate(),
                nextResetAt
        );
    }
}
