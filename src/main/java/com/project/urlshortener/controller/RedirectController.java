package com.project.urlshortener.controller;

import com.project.urlshortener.model.Url;
import com.project.urlshortener.service.AnalyticsEventService;
import com.project.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RequiredArgsConstructor
@RestController
public class RedirectController {

    private final UrlService urlService;
    private final AnalyticsEventService analyticsEventService;

    @GetMapping("/{shortLink}")
    public ResponseEntity<Void> redirect(@PathVariable String shortLink,
                                         HttpServletRequest request) {
        Url url = urlService.getEncodedUrl(shortLink);

        // Record rich click event (device, location, referrer) — non-blocking
        analyticsEventService.recordClick(shortLink, request);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url.getOriginalUrl()));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
