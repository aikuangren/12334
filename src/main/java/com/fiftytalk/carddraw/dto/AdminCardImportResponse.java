package com.fiftytalk.carddraw.dto;

import java.util.List;
import java.util.Map;

public record AdminCardImportResponse(
        Integer totalRows,
        Integer createdCount,
        Integer skippedCount,
        Map<String, Integer> createdByRarity,
        List<String> errors
) {
}
