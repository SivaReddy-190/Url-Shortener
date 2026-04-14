package com.project.urlshortener.service;

import com.project.urlshortener.model.Url;
import com.project.urlshortener.model.UrlDto;

public interface UrlService {

    Url generateShortLink(UrlDto urlDto);

    Url persistShortLink(Url url);

    Url getEncodedUrl(String shortLink);

    Url getOriginalUrl(String shortLink);

    void deleteShortLink(Url url);

}