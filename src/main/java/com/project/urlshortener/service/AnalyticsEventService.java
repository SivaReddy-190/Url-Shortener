package com.project.urlshortener.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Captures click event metadata from the HTTP request and
 * asynchronously persists it to the database in batches.
 */
public interface AnalyticsEventService {

    /**
     * Record a click event. Extracts device, referrer, and IP metadata
     * from the request and queues the event for batch persistence.
     * This method returns immediately and never blocks the redirect.
     */
    void recordClick(String shortLink, HttpServletRequest request);

    /**
     * Flush any buffered events to the database immediately.
     * Called on shutdown to prevent data loss.
     */
    void flush();
}
