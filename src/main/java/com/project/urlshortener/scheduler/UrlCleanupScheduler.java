package com.project.urlshortener.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import com.project.urlshortener.repository.UrlRepository;

@Component
public class UrlCleanupScheduler {

    private final UrlRepository urlRepository;
    public final Logger log = LoggerFactory.getLogger(UrlCleanupScheduler.class);

    UrlCleanupScheduler(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @Scheduled(cron = "0 0 16 * * ?")
    public void cleanupExpiredUrls() {

        log.info("Starting cleanup of expired URLs at {}", LocalDateTime.now());
        try{
            int deletedCount = urlRepository.deleteByExpirationTimeBefore(LocalDateTime.now());
            log.info("Cleanup completed successfully at {}", LocalDateTime.now());
            log.info("Deleted {} expired URLs", deletedCount);
        }
        catch(Exception e) {
            log.error("Error while cleaning up expired URLs", e);
        }
    }

}
