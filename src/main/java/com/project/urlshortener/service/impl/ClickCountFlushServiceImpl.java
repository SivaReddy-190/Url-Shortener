package com.project.urlshortener.service.impl;

import com.project.urlshortener.repository.UrlRepository;
import com.project.urlshortener.service.ClickCountFlushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ClickCountFlushServiceImpl implements ClickCountFlushService {


    private final UrlRepository urlRepository;

    /**
     * Flush a single key's click delta to PostgreSQL within its own transaction.
     */
    @Transactional
    @Override
    public void flushSingleKey(String shortLink, long delta) {
        urlRepository.incrementClickCountByDelta(shortLink, delta);
    }
}
