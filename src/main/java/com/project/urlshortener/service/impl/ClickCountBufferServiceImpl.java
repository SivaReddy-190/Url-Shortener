package com.project.urlshortener.service.impl;

import com.project.urlshortener.service.ClickCountBufferService;
import com.project.urlshortener.service.ClickCountFlushService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClickCountBufferServiceImpl implements ClickCountBufferService {

    private static final String CLICK_KEY_PREFIX = "url:clicks:";

    /**
     * Lua script: atomically read the click counter and delete the key.
     * Returns the value (or nil if key didn't exist).
     * This ensures no clicks are lost or double-counted between GET and DELETE.
     */
    private static final String ATOMIC_GET_AND_DELETE_SCRIPT =
            "local val = redis.call('GET', KEYS[1]) " +
            "if val then redis.call('DEL', KEYS[1]) end " +
            "return val";

    private final ClickCountFlushService clickCountFlushService;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Atomically increment the click counter in Redis.
     * Redis INCR is single-threaded and atomic — no lost increments even under concurrency.
     * The count persists in Redis independently of the JVM, surviving app crashes.
     */
    @Override
    public void increment(String shortLink) {
        try {
            redisTemplate.opsForValue().increment(CLICK_KEY_PREFIX + shortLink);
        } catch (Exception e) {
            log.error("Failed to increment Redis click counter for '{}', click may be lost", shortLink, e);
        }
    }

    /**
     * Flush accumulated clicks from Redis to PostgreSQL.
     * Runs on a configurable schedule (default every 5 minutes).
     * For each click counter key, atomically reads and deletes the value via Lua script,
     * then applies the delta to PostgreSQL.
     *
     * Note: NOT wrapped in a single @Transactional — each key is flushed independently
     * so that a single DB failure doesn't roll back all previously flushed counts.
     */
    @Scheduled(fixedRateString = "${click-count.flush-interval-ms:300000}")
    @Override
    public void flushToDatabase() {
        // Use SCAN instead of KEYS to avoid blocking Redis in production
        Set<String> keys = scanKeys(CLICK_KEY_PREFIX + "*");
        if (keys.isEmpty()) {
            return;
        }

        int flushedCount = 0;
        long totalClicks = 0;

        DefaultRedisScript<String> script = new DefaultRedisScript<>(ATOMIC_GET_AND_DELETE_SCRIPT, String.class);

        for (String key : keys) {
            String shortLink = key.substring(CLICK_KEY_PREFIX.length());
            long delta = 0;
            try {
                // Atomically read and delete the counter
                String value = redisTemplate.execute(script, Collections.singletonList(key));
                if (value == null) {
                    continue;
                }

                delta = Long.parseLong(value);
                if (delta > 0) {
                    // Delegate to a separate bean so @Transactional is applied via Spring's AOP proxy
                    clickCountFlushService.flushSingleKey(shortLink, delta);
                    flushedCount++;
                    totalClicks += delta;
                }
            } catch (Exception e) {
                // If DB write fails, re-add the delta to Redis so it's retried next flush
                log.error("Failed to flush click count for '{}', re-queuing {} clicks", shortLink, delta, e);
                if (delta > 0) {
                    try {
                        // Restore the delta we already extracted — don't re-read from Redis
                        redisTemplate.opsForValue().increment(key, delta);
                    } catch (Exception restoreEx) {
                        log.error("CRITICAL: Failed to restore {} clicks for '{}' in Redis. Data lost.",
                                delta, shortLink, restoreEx);
                    }
                }
            }
        }

        if (flushedCount > 0) {
            log.info("Flushed {} click counters to database ({} total clicks)", flushedCount, totalClicks);
        }
    }

    /**
     * Flush remaining clicks on graceful shutdown so nothing is lost.
     */
    @PreDestroy
    @Override
    public void shutdown() {
        log.info("Shutting down — flushing remaining click counts to database");
        flushToDatabase();
    }

    /**
     * Returns the current buffered click count from Redis for a given short link.
     * Used by the stats endpoint to provide real-time accuracy (DB count + this delta).
     */
    @Override
    public long getBufferedCount(String shortLink) {
        try {
            String value = redisTemplate.opsForValue().get(CLICK_KEY_PREFIX + shortLink);
            return value != null ? Long.parseLong(value) : 0;
        } catch (Exception e) {
            log.warn("Failed to read Redis click counter for '{}', returning 0", shortLink, e);
            return 0;
        }
    }

    /**
     * Uses Redis SCAN (cursor-based) instead of KEYS to find matching keys.
     * SCAN is non-blocking and safe for production use with large key spaces.
     */
    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                cursor.forEachRemaining(keys::add);
            }
        } catch (Exception e) {
            log.error("Failed to scan Redis keys with pattern '{}'", pattern, e);
        }
        return keys;
    }
}

