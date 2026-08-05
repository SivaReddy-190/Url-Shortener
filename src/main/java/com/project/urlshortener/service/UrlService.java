package com.project.urlshortener.service;

import com.project.urlshortener.dto.UrlDto;
import com.project.urlshortener.model.Url;

public interface UrlService {

    Url generateShortLink(UrlDto urlDto);

    Url getEncodedUrl(String shortLink);

    Url getOriginalUrl(String shortLink);

    void deleteShortLink(String shortLink);

}