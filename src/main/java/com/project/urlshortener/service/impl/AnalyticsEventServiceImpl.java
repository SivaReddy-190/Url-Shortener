package com.project.urlshortener.service.impl;

import com.project.urlshortener.model.ClickEvent;
import com.project.urlshortener.repository.ClickEventRepository;
import com.project.urlshortener.service.AnalyticsEventService;
import com.project.urlshortener.service.GeoLocationService;
import com.project.urlshortener.service.GeoLocationService.GeoLocation;
import com.project.urlshortener.util.UserAgentParser;
import com.project.urlshortener.util.UserAgentParser.ParsedUserAgent;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class AnalyticsEventServiceImpl implements AnalyticsEventService {

    private final ClickEventRepository clickEventRepository;
    private final GeoLocationService geoLocationService;
    private final int batchSize;

    /**
     * Lock-free queue — producers (redirect threads) never block.
     * The scheduled flush drains it in batches.
     */
    private final ConcurrentLinkedQueue<ClickEvent> eventBuffer = new ConcurrentLinkedQueue<>();

    public AnalyticsEventServiceImpl(
            ClickEventRepository clickEventRepository,
            GeoLocationService geoLocationService,
            @Value("${analytics.batch-size:500}") int batchSize) {
        this.clickEventRepository = clickEventRepository;
        this.geoLocationService = geoLocationService;
        this.batchSize = batchSize;
    }

    @Override
    public void recordClick(String shortLink, HttpServletRequest request) {
        try {
            String ip = extractClientIp(request);
            String userAgentStr = request.getHeader("User-Agent");
            String referer = request.getHeader("Referer");

            ParsedUserAgent ua = UserAgentParser.parse(userAgentStr);
            GeoLocation geo = geoLocationService.locate(ip);

            ClickEvent event = ClickEvent.builder()
                    .shortLink(shortLink)
                    .clickedAt(Instant.now())
                    .ipAddress(ip)
                    .countryCode(geo.countryCode())
                    .city(geo.city())
                    .deviceType(ua.deviceType())
                    .osName(ua.osName())
                    .osVersion(ua.osVersion())
                    .browserName(ua.browserName())
                    .browserVersion(ua.browserVersion())
                    .referrerUrl(referer)
                    .referrerDomain(extractDomain(referer))
                    .userAgent(userAgentStr)
                    .build();

            eventBuffer.add(event);
        } catch (Exception e) {
            log.error("Failed to capture click event for '{}', event dropped", shortLink, e);
        }
    }

    /**
     * Flush buffered events to the database every 5 seconds.
     * Drains up to batchSize events per cycle to bound memory and DB load.
     */
    @Scheduled(fixedRateString = "${analytics.flush-interval-ms:5000}")
    @Override
    public void flush() {
        List<ClickEvent> batch = new ArrayList<>(batchSize);
        ClickEvent event;
        while ((event = eventBuffer.poll()) != null && batch.size() < batchSize) {
            batch.add(event);
        }

        if (batch.isEmpty()) {
            return;
        }

        try {
            clickEventRepository.saveAll(batch);
            log.debug("Flushed {} click events to database", batch.size());
        } catch (Exception e) {
            log.error("Failed to flush {} click events — re-queuing for retry", batch.size(), e);
            eventBuffer.addAll(batch);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down — flushing remaining analytics events to database");
        List<ClickEvent> remaining = new ArrayList<>();
        ClickEvent event;
        while ((event = eventBuffer.poll()) != null) {
            remaining.add(event);
        }
        if (!remaining.isEmpty()) {
            try {
                clickEventRepository.saveAll(remaining);
                log.info("Flushed {} remaining click events on shutdown", remaining.size());
            } catch (Exception e) {
                log.error("CRITICAL: Failed to flush {} events on shutdown — data lost",
                        remaining.size(), e);
            }
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String extractDomain(String referrer) {
        if (referrer == null || referrer.isBlank()) {
            return null;
        }
        try {
            return URI.create(referrer).getHost();
        } catch (Exception e) {
            return null;
        }
    }
}
