package com.project.urlshortener.service;

import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

public interface ClickCountBufferService {
    void increment(String shortLink);

    @Scheduled(fixedRate = 10000)
    @Transactional
    void flushToDatabase();

    @PreDestroy
    void shutdown();

    long getBufferedCount(String shortLink);
}
