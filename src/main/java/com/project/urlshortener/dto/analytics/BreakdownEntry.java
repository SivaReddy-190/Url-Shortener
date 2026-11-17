package com.project.urlshortener.dto.analytics;

/**
 * A single category to count entry in a breakdown chart
 * (e.g., device type, browser, country, referrer).
 */
public record BreakdownEntry(
        String name,
        long clicks
) {}
