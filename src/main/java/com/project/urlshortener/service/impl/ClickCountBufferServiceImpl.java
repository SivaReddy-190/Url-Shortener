package com.project.urlshortener.service.impl;

import com.project.urlshortener.repository.UrlRepository;
import com.project.urlshortener.service.ClickCountBufferService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClickCountBufferServiceImpl implements ClickCountBufferService {

    private final UrlRepository urlRepository;
    private final ConcurrentHashMap<String, AtomicLong> clickBuffer = new ConcurrentHashMap<>();

    /**
     * Accumulate a click in memory. This is O(1) and lock-free.
     */
    @Override
    public void increment(String shortLink) {
        clickBuffer.computeIfAbsent(shortLink, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Flush accumulated clicks to the database.
     * Runs every 10 seconds. Each short link gets a single UPDATE with the accumulated delta.
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    @Override
    public void flushToDatabase() {
        if (clickBuffer.isEmpty()) {
            return;
        }

        // Snapshot and drain: iterate all keys and atomically reset each counter
        for (Map.Entry<String, AtomicLong> entry : clickBuffer.entrySet()) {
            String shortLink = entry.getKey();
            long delta = entry.getValue().getAndSet(0);
            if (delta > 0) {
                try {
                    urlRepository.incrementClickCountByDelta(shortLink, delta);
                } catch (Exception e) {
                    // If the DB write fails, re-add the delta so it's retried next flush
                    clickBuffer.computeIfAbsent(shortLink, k -> new AtomicLong(0)).addAndGet(delta);
                    log.error("Failed to flush click count for '{}', re-queued {} clicks", shortLink, delta, e);
                }
            }
        }

        // Remove entries that are now zero to prevent unbounded map growth
        clickBuffer.entrySet().removeIf(entry -> entry.getValue().get() == 0);
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
     * Visible for testing: returns the current buffered count for a short link.
     */
    @Override
    public long getBufferedCount(String shortLink) {
        AtomicLong count = clickBuffer.get(shortLink);
        return count != null ? count.get() : 0;
    }
}
