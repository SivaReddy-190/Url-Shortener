package com.project.urlshortener.service;

import org.springframework.transaction.annotation.Transactional;

public interface ClickCountFlushService {
    @Transactional
    void flushSingleKey(String shortLink, long delta);
}
