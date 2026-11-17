package com.project.urlshortener.service;

/**
 * Resolves an IP address to a geographic location.
 * Default implementation returns unknown; swap in MaxMind GeoLite2 for production.
 */
public interface GeoLocationService {

    record GeoLocation(String countryCode, String city) {}

    GeoLocation locate(String ipAddress);
}
