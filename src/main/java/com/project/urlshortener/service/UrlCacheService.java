package com.project.urlshortener.service;

import com.project.urlshortener.model.Url;

import java.util.Optional;

public interface UrlCacheService {

    String CACHE_KEY_PREFIX = "url:cache:";

    Optional<Url> getCachedUrl(String shortLink);

    void cacheUrl(Url url);

    void evict(String shortLink);
}
