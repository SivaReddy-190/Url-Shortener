package com.project.urlshortener.config;

import com.project.urlshortener.service.ClickCountBufferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs on application startup to flush any leftover click counts in Redis
 * that were not flushed before the previous instance shut down (e.g., crash, kill -9).
 *
 * This ensures zero data loss across restarts — any counts accumulated in Redis
 * before the crash are immediately persisted to PostgreSQL.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RedisStartupRecovery implements ApplicationRunner {

    private final ClickCountBufferService clickCountBufferService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Startup recovery — flushing any leftover Redis click counts to database");
        try {
            clickCountBufferService.flushToDatabase();
            log.info("Startup recovery completed successfully");
        } catch (Exception e) {
            log.error("Startup recovery failed — leftover counts will be flushed on next scheduled run", e);
        }
    }
}
