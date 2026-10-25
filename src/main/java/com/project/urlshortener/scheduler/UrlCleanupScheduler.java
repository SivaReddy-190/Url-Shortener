package com.project.urlshortener.scheduler;

import com.project.urlshortener.model.Url;
import com.project.urlshortener.repository.UrlRepository;
import com.project.urlshortener.service.UrlCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class UrlCleanupScheduler {

    private final UrlRepository urlRepository;
    private final UrlCacheService urlCacheService;

    @Scheduled(cron = "0 0 16 * * ?")
    @Transactional
    public void cleanupExpiredUrls() {
        log.info("Starting cleanup of expired URLs");
        try {
            // Find expired URLs first so we can evict their Redis cache entries
            List<Url> expiredUrls = urlRepository.findByExpirationTimeBefore(LocalDateTime.now());
            if (expiredUrls.isEmpty()) {
                log.info("Cleanup completed — no expired URLs found");
                return;
            }

            // Evict Redis cache entries for each expired URL
            for (Url url : expiredUrls) {
                try {
                    urlCacheService.evict(url.getShortLink());
                } catch (Exception e) {
                    log.warn("Failed to evict cache for expired short link '{}'", url.getShortLink(), e);
                }
            }

            // Delete expired URLs from database
            int deletedCount = urlRepository.deleteByExpirationTimeBefore(LocalDateTime.now());
            log.info("Cleanup completed — deleted {} expired URLs", deletedCount);
        } catch (Exception e) {
            log.error("Error while cleaning up expired URLs", e);
        }
    }
}

