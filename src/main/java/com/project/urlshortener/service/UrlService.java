package com.project.urlshortener.service;

import com.project.urlshortener.dto.url.UrlDto;
import com.project.urlshortener.model.Url;
import com.project.urlshortener.model.User;

import java.util.List;

public interface UrlService {

    Url generateShortLink(UrlDto urlDto, User user);

    Url getEncodedUrl(String shortLink);

    Url getOriginalUrl(String shortLink, User user);

    void deleteShortLink(String shortLink, User user);

    List<Url> getUserLinks(User user);

}