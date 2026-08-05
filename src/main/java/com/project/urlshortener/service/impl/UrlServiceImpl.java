package com.project.urlshortener.service;

import com.google.common.hash.Hashing;
import com.project.urlshortener.model.Url;
import com.project.urlshortener.model.UrlDto;
import com.project.urlshortener.repository.UrlRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class UrlServiceImpl implements UrlService {

    private final UrlRepository repository;

    @Override
    public Url generateShortLink(UrlDto urlDto) {

        if(StringUtils.isNotEmpty(urlDto.getUrl())) {
            String shortLink;
            if (StringUtils.isNotBlank(urlDto.getShortLink())) {
                shortLink = urlDto.getShortLink();
                if (repository.findByShortLink(shortLink).isPresent()) {
                    throw new IllegalArgumentException("shortLink is taken, enter another");
                }
            } else {
                shortLink = generateUniqueShortLink(urlDto.getUrl());
            }

            Url urlToPersist = new Url();
            urlToPersist.setShortLink(shortLink);
            urlToPersist.setOriginalUrl(urlDto.getUrl());
            urlToPersist.setCreatedTime(LocalDateTime.now());
            urlToPersist.setExpirationTime(getExpirationTime(urlDto.getExpirationDays(), urlToPersist.getCreatedTime()));
            return persistShortLink(urlToPersist);
        }
        return null;
    }

    private LocalDateTime getExpirationTime(Integer expirationDays, LocalDateTime createdTime) {
        if(expirationDays == null || expirationDays <= 0) return createdTime.plusDays(60);

        return createdTime.plusDays(expirationDays);
    }

    private String generateUniqueShortLink(String url) {
        String encodedUrl;
        int attempts = 0;
        do {
            LocalDateTime time = LocalDateTime.now();
            encodedUrl = Hashing.murmur3_32_fixed()
                    .hashString(url + time.toString() + attempts, StandardCharsets.UTF_8)
                    .toString();
            attempts++;
        } while (repository.findByShortLink(encodedUrl).isPresent() && attempts < 5);
        return encodedUrl;
    }

    @Override
    public Url persistShortLink(Url url) {
        return repository.save(url);
    }

    @Override
    @Transactional
    public Url getEncodedUrl(String shortLink) {

        Url url = repository.findByShortLink(shortLink).orElse(null);
        
        if(url != null) {
            repository.incrementClickCount(shortLink);
        }
        return url;
    }

    @Override
    public Url getOriginalUrl(String shortLink) {
        return repository.findByShortLink(shortLink).orElse(null);
    }

    @Override
    public void deleteShortLink(Url url) {
        repository.delete(url);
    }

}
