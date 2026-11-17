package com.project.urlshortener.dto.analytics;

import java.util.List;

/**
 * Full dashboard payload for a single short link.
 */
public record DashboardResponse(
        String shortLink,
        String originalUrl,
        long totalClicks,
        long clicksLast24h,
        long clicksLast7d,
        long clicksLast30d,
        List<TimeSeriesPoint> clicksByDate,
        List<BreakdownEntry> deviceBreakdown,
        List<BreakdownEntry> browserBreakdown,
        List<BreakdownEntry> osBreakdown,
        List<BreakdownEntry> countryBreakdown,
        List<BreakdownEntry> topReferrers
) {}
