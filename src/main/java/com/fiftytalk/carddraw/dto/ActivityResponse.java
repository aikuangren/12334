package com.fiftytalk.carddraw.dto;

public record ActivityResponse(
        Long activityId,
        String activityName,
        Long poolId,
        String poolVersion,
        QuotaDto quota,
        Long totalDrawCount,
        String buttonText,
        String tipText
) {
}
