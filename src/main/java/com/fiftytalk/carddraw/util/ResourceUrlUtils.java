package com.fiftytalk.carddraw.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class ResourceUrlUtils {
    private ResourceUrlUtils() {
    }

    public static String cdnUrl(String baseUrl, String relativePath) {
        String normalizedBase = normalizeBaseUrl(baseUrl);
        String normalizedPath = relativePath == null ? "" : relativePath.replace('\\', '/');
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return normalizedBase + encodePath(normalizedPath);
    }

    public static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null || baseUrl.isBlank() ? "https://cdn.51talkjr.com/" : baseUrl.trim();
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private static String encodePath(String path) {
        String[] parts = path.split("/", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = URLEncoder.encode(parts[i], StandardCharsets.UTF_8).replace("+", "%20");
        }
        return String.join("/", parts);
    }
}
