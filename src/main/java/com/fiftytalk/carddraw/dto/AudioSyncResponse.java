package com.fiftytalk.carddraw.dto;

import java.util.List;

public record AudioSyncResponse(
        int fileCount,
        int matchedWordCount,
        int updatedCardCount,
        List<String> unmatchedFiles
) {
}
