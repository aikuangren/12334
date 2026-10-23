package com.fiftytalk.carddraw.dto;

public record ResourceUploadResponse(
        String url,
        String localUrl,
        String fileName,
        long size
) {
}
