package com.project.urlshortener.scheduler;

import com.project.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class UrlCleanupScheduler {

    private final UrlRepository urlRepository;

    @Scheduled(cron = "0 0 16 * * ?")
    public void cleanupExpiredUrls() {
        log.info("Starting cleanup of expired URLs");
        try {
            int deletedCount = urlRepository.deleteByExpirationTimeBefore(LocalDateTime.now());
            log.info("Cleanup completed — deleted {} expired URLs", deletedCount);
        } catch (Exception e) {
            log.error("Error while cleaning up expired URLs", e);
        }
    }
}
