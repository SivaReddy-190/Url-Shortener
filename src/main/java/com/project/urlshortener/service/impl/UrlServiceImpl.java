package com.project.urlshortener.service.impl;

import com.project.urlshortener.dto.url.UrlDto;
import com.project.urlshortener.exception.AccessDeniedException;
import com.project.urlshortener.exception.ShortLinkCollisionException;
import com.project.urlshortener.exception.UrlExpiredException;
import com.project.urlshortener.exception.UrlNotFoundException;
import com.project.urlshortener.model.Url;
import com.project.urlshortener.model.User;
import com.project.urlshortener.repository.UrlRepository;
import com.project.urlshortener.service.ClickCountBufferService;
import com.project.urlshortener.service.UrlCacheService;
import com.project.urlshortener.service.UrlService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UrlServiceImpl implements UrlService {

    private static final int DEFAULT_EXPIRATION_DAYS = 60;
    private static final int MAX_COLLISION_RETRIES = 5;
    private static final int SHORT_LINK_LENGTH = 8;

    /** Hosts that must never be shortened (cloud metadata, localhost, etc.) */
    private static final List<String> BLOCKED_HOSTS = List.of(
            "169.254.169.254",          // AWS/GCP metadata
            "metadata.google.internal", // GCP metadata
            "metadata.internal"         // generic cloud metadata
    );

    private final UrlRepository repository;
    private final ClickCountBufferService clickCountBufferService;
    private final UrlCacheService urlCacheService;

    @Override
    public Url generateShortLink(UrlDto urlDto, User user) {
        // Defense-in-depth: validate URL scheme and host (DTO @Pattern is the first layer)
        validateUrl(urlDto.url());

        String shortLink;

        if (urlDto.shortLink() != null && !urlDto.shortLink().isBlank()) {
            shortLink = urlDto.shortLink();
            if (repository.findByShortLink(shortLink).isPresent()) {
                throw new IllegalArgumentException("Short link '" + shortLink + "' is already taken, choose another");
            }
        } else {
            shortLink = generateUniqueShortLink();
        }

        LocalDateTime now = LocalDateTime.now();
        Url urlToPersist = Url.builder()
                .shortLink(shortLink)
                .originalUrl(urlDto.url())
                .createdTime(now)
                .expirationTime(getExpirationTime(urlDto.expirationDays(), now))
                .clickCount(0L)
                .user(user)
                .build();

        Url saved = repository.save(urlToPersist);

        // Cache the newly created URL entity in Redis
        urlCacheService.cacheUrl(saved);

        return saved;
    }

    @Transactional
    @Override
    public Url getEncodedUrl(String shortLink) {
        // Check Redis cache first, fall back to PostgreSQL on miss
        Url url = urlCacheService.getCachedUrl(shortLink)
                .orElseGet(() -> {
                    Url fromDb = repository.findByShortLink(shortLink)
                            .orElseThrow(() -> new UrlNotFoundException("URL not found for short link: " + shortLink));
                    // Cache the entity for subsequent requests
                    urlCacheService.cacheUrl(fromDb);
                    return fromDb;
                });

        if (url.getExpirationTime().isBefore(LocalDateTime.now())) {
            repository.delete(url);
            urlCacheService.evict(shortLink);
            throw new UrlExpiredException("Short link '" + shortLink + "' has expired");
        }

        // Atomic increment in Redis — survives JVM crashes
        clickCountBufferService.increment(shortLink);
        return url;
    }

    @Override
    public Url getOriginalUrl(String shortLink, User user) {
        Url url = repository.findByShortLink(shortLink)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for short link: " + shortLink));
        verifyOwnership(url, user);

        // Add unflushed Redis buffer to provide real-time accurate stats
        long bufferedClicks = clickCountBufferService.getBufferedCount(shortLink);
        url.setClickCount(url.getClickCount() + bufferedClicks);

        return url;
    }

    @Override
    public void deleteShortLink(String shortLink, User user) {
        Url url = repository.findByShortLink(shortLink)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for short link: " + shortLink));
        verifyOwnership(url, user);

        // Clean up Redis: evict cached entity and remove click counter
        urlCacheService.evict(shortLink);

        repository.delete(url);
    }

    @Override
    @Transactional
    public Url extendExpiration(String shortLink, int additionalDays, User user) {
        if (additionalDays <= 0) {
            throw new IllegalArgumentException("Additional days must be greater than 0");
        }
        Url url = repository.findByShortLink(shortLink)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for short link: " + shortLink));
        verifyOwnership(url, user);

        LocalDateTime currentExpiry = url.getExpirationTime();
        LocalDateTime base = (currentExpiry == null || currentExpiry.isBefore(LocalDateTime.now()))
                ? LocalDateTime.now()
                : currentExpiry;

        url.setExpirationTime(base.plusDays(additionalDays));
        urlCacheService.evict(shortLink);

        return repository.save(url);
    }

    @Override
    public List<Url> getUserLinks(User user) {
        return repository.findByUser(user);
    }

    private void verifyOwnership(Url url, User user) {
        if (url.getUser() == null || !url.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this link");
        }
    }

    private LocalDateTime getExpirationTime(Integer expirationDays, LocalDateTime createdTime) {
        if (expirationDays == null || expirationDays <= 0) {
            return createdTime.plusDays(DEFAULT_EXPIRATION_DAYS);
        }
        return createdTime.plusDays(expirationDays);
    }

    private String generateUniqueShortLink() {
        for (int attempt = 0; attempt < MAX_COLLISION_RETRIES; attempt++) {
            String shortLink = UUID.randomUUID().toString().replace("-", "")
                    .substring(0, SHORT_LINK_LENGTH);
            if (repository.findByShortLink(shortLink).isEmpty()) {
                return shortLink;
            }
        }
        throw new ShortLinkCollisionException(
                "Failed to generate a unique short link after " + MAX_COLLISION_RETRIES + " attempts");
    }

    /**
     * Validates that the URL uses a safe scheme (http/https) and does not point to
     * internal/private network addresses (SSRF prevention) or cloud metadata endpoints.
     */
    private void validateUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL format");
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only http and https URLs are allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL must have a valid host");
        }

        // Block known cloud metadata endpoints
        if (BLOCKED_HOSTS.contains(host.toLowerCase())) {
            throw new IllegalArgumentException("This URL is not allowed");
        }

        // Block private, loopback, and link-local IP addresses (SSRF prevention)
        try {
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
                throw new IllegalArgumentException("URLs pointing to private/internal networks are not allowed");
            }
        } catch (IllegalArgumentException e) {
            throw e; // re-throw our own validation exceptions
        } catch (Exception e) {
            log.warn("Could not resolve host '{}' for SSRF check, allowing URL", host);
        }
    }
}

