package com.project.urlshortener.service.impl;

import com.project.urlshortener.service.GeoLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub GeoLocation implementation — returns unknown for all IPs.
 *
 * To enable real geo-lookup:
 *   1. Add com.maxmind.geoip2:geoip2 dependency
 *   2. Download GeoLite2-City.mmdb from MaxMind
 *   3. Replace this class with a MaxMind-backed implementation
 */
@Slf4j
@Service
public class GeoLocationServiceImpl implements GeoLocationService {

    @Override
    public GeoLocation locate(String ipAddress) {
        // TODO: Integrate MaxMind GeoLite2 for real geo-lookup
        return new GeoLocation(null, null);
    }
}
