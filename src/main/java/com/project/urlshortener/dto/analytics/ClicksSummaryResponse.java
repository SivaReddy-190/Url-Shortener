package com.project.urlshortener.dto.analytics;

/**
 * Lightweight click count summary (used for quick stats).
 */
public record ClicksSummaryResponse(
        String shortLink,
        long totalClicks,
        long clicksLast24h,
        long clicksLast7d,
        long clicksLast30d
) {}
