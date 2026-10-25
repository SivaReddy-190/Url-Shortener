package com.project.urlshortener.service.impl;

import com.project.urlshortener.model.Url;
import com.project.urlshortener.service.UrlCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UrlCacheServiceImpl implements UrlCacheService {

    private final RedisTemplate<String, Object> redisObjectTemplate;
    private final long ttlSeconds;

    public UrlCacheServiceImpl(
            @Qualifier("redisObjectTemplate") RedisTemplate<String, Object> redisObjectTemplate,
            @Value("${url-cache.ttl-seconds:1800}") long ttlSeconds) {
        this.redisObjectTemplate = redisObjectTemplate;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Retrieve a cached URL entity from Redis.
     *
     * @param shortLink the short link key
     * @return the cached Url wrapped in Optional, or empty if not cached
     */
    @Override
    public Optional<Url> getCachedUrl(String shortLink) {
        try {
            Object cached = redisObjectTemplate.opsForValue().get(CACHE_KEY_PREFIX + shortLink);
            if (cached instanceof Url url) {
                return Optional.of(url);
            }
        } catch (Exception e) {
            log.warn("Failed to read URL cache for '{}', falling through to DB", shortLink, e);
        }
        return Optional.empty();
    }

    /**
     * Cache a URL entity in Redis with the configured TTL.
     *
     * @param url the URL entity to cache
     */
    @Override
    public void cacheUrl(Url url) {
        try {
            redisObjectTemplate.opsForValue().set(
                    CACHE_KEY_PREFIX + url.getShortLink(),
                    url,
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.warn("Failed to cache URL for '{}'", url.getShortLink(), e);
        }
    }

    /**
     * Evict a cached URL entity from Redis.
     *
     * @param shortLink the short link key to evict
     */
    @Override
    public void evict(String shortLink) {
        try {
            redisObjectTemplate.delete(CACHE_KEY_PREFIX + shortLink);
        } catch (Exception e) {
            log.warn("Failed to evict URL cache for '{}'", shortLink, e);
        }
    }
}
