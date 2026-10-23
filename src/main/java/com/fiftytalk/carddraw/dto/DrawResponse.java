package com.fiftytalk.carddraw.dto;

import java.util.List;

public record DrawResponse(
        Long orderId,
        Boolean hasNewCard,
        List<DrawCardResult> cards,
        QuotaDto quota,
        Long totalDrawCount
) {
}
