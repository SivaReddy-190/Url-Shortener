package com.project.urlshortener.service.impl;

import com.project.urlshortener.dto.UrlDto;
import com.project.urlshortener.exception.ShortLinkCollisionException;
import com.project.urlshortener.exception.UrlExpiredException;
import com.project.urlshortener.exception.UrlNotFoundException;
import com.project.urlshortener.model.Url;
import com.project.urlshortener.repository.UrlRepository;
import com.project.urlshortener.service.UrlService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UrlServiceImpl implements UrlService {

    private static final int DEFAULT_EXPIRATION_DAYS = 60;
    private static final int MAX_COLLISION_RETRIES = 5;
    private static final int SHORT_LINK_LENGTH = 8;

    private final UrlRepository repository;

    @Override
    public Url generateShortLink(UrlDto urlDto) {
        String shortLink;

        if (urlDto.shortLink() != null && !urlDto.shortLink().isBlank()) {
            shortLink = urlDto.shortLink();
            if (repository.findByShortLink(shortLink).isPresent()) {
                throw new IllegalArgumentException("Short link '" + shortLink + "' is already taken, choose another");
            }
        } else {
            shortLink = generateUniqueShortLink();
        }

        LocalDateTime now = LocalDateTime.now();
        Url urlToPersist = Url.builder()
                .shortLink(shortLink)
                .originalUrl(urlDto.url())
                .createdTime(now)
                .expirationTime(getExpirationTime(urlDto.expirationDays(), now))
                .clickCount(0L)
                .build();

        return repository.save(urlToPersist);
    }

    @Override
    @Transactional
    public Url getEncodedUrl(String shortLink) {
        Url url = repository.findByShortLink(shortLink)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for short link: " + shortLink));

        if (url.getExpirationTime().isBefore(LocalDateTime.now())) {
            repository.delete(url);
            throw new UrlExpiredException("Short link '" + shortLink + "' has expired");
        }

        repository.incrementClickCount(shortLink);
        return url;
    }

    @Override
    public Url getOriginalUrl(String shortLink) {
        return repository.findByShortLink(shortLink)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for short link: " + shortLink));
    }

    @Override
    public void deleteShortLink(String shortLink) {
        Url url = repository.findByShortLink(shortLink)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for short link: " + shortLink));
        repository.delete(url);
    }

    private LocalDateTime getExpirationTime(Integer expirationDays, LocalDateTime createdTime) {
        if (expirationDays == null || expirationDays <= 0) {
            return createdTime.plusDays(DEFAULT_EXPIRATION_DAYS);
        }
        return createdTime.plusDays(expirationDays);
    }

    private String generateUniqueShortLink() {
        for (int attempt = 0; attempt < MAX_COLLISION_RETRIES; attempt++) {
            String shortLink = UUID.randomUUID().toString().replace("-", "")
                    .substring(0, SHORT_LINK_LENGTH);
            if (repository.findByShortLink(shortLink).isEmpty()) {
                return shortLink;
            }
        }
        throw new ShortLinkCollisionException(
                "Failed to generate a unique short link after " + MAX_COLLISION_RETRIES + " attempts");
    }
}
