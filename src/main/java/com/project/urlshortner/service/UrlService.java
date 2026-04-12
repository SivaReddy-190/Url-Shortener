package com.project.urlshortner.service;

import com.google.common.hash.Hashing;
import com.project.urlshortner.model.Url;
import com.project.urlshortner.model.UrlDto;
import com.project.urlshortner.repository.UrlRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class UrlService {

    @Autowired
    private UrlRepository repository;

    public Url generateShortLink(UrlDto urlDto) {

        if(StringUtils.isNotEmpty(urlDto.getUrl())) {
            String encodeUrl = encodeUrl(urlDto.getUrl());
            Url urlToPersist = new Url();
            urlToPersist.setShortLink(encodeUrl);
            urlToPersist.setOriginalUrl(urlDto.getUrl());
            urlToPersist.setCreatedTime(LocalDateTime.now());
            urlToPersist.setExpirationTime(getExpirationTime(urlDto.getExpirationDate(),urlToPersist.getCreatedTime()));
            Url urlToRet = persistShortLink(urlToPersist);

            if(urlToRet!=null) return urlToRet;
            return null;
        }
        return null;


    }

    private LocalDateTime getExpirationTime(String expirationDate, LocalDateTime createdTime) {
        if(StringUtils.isBlank(expirationDate)) return  createdTime.plusDays(60);

        return LocalDateTime.parse(expirationDate);
    }

    private String encodeUrl(String url) {
        String encodedUrl = "";
        LocalDateTime time = LocalDateTime.now();
        encodedUrl = Hashing.murmur3_32_fixed().hashString(time.toString(), StandardCharsets.UTF_8).toString();
        return encodedUrl;
    }

    public Url persistShortLink(Url url) {
        return repository.save(url);
    }

    @Transactional
    public Url getEncodedUrl(String shortLink) {

        Url url = repository.findByShortLink(shortLink)
                .orElse(null);
        if(url != null) {
            repository.incrementClickCount(shortLink);
        }
        return url;
    }

    public Url getOriginalUrl(String shortLink) {
        return repository.findByShortLink(shortLink).orElse(null);
    }

    public void deleteShortLink(Url url) {
        repository.delete(url);
    }

}
