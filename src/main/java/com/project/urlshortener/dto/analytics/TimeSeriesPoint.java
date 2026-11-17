package com.project.urlshortener.dto.analytics;

/**
 * A single data point in a time-series (date or hour bucket to count).
 */
public record TimeSeriesPoint(
        String timestamp,
        long clicks
) {}
