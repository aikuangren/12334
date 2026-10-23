package com.fiftytalk.carddraw.dto;

import com.fiftytalk.carddraw.domain.CostType;

import java.time.LocalDateTime;
import java.util.List;

public record HistoryResponse(
        Long orderId,
        Integer drawNo,
        CostType costType,
        Boolean hasNewCard,
        LocalDateTime createdAt,
        List<DrawCardResult> cards
) {
}

