package com.project.urlshortener.controller;

import com.project.urlshortener.dto.url.ExtendExpiryRequest;
import com.project.urlshortener.dto.url.UrlDto;
import com.project.urlshortener.dto.url.UrlResponseDto;
import com.project.urlshortener.model.Url;
import com.project.urlshortener.model.User;
import com.project.urlshortener.service.UrlService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlService service;

    @PostMapping("/generate")
    public ResponseEntity<UrlResponseDto> generateShortLink(
            @Valid @RequestBody UrlDto urlDto,
            @AuthenticationPrincipal User user) {
        Url url = service.generateShortLink(urlDto, user);
        UrlResponseDto response = new UrlResponseDto(
                url.getOriginalUrl(),
                url.getShortLink(),
                url.getExpirationTime()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/{shortLink}")
    public ResponseEntity<Map<String, Long>> getStats(
            @PathVariable String shortLink,
            @AuthenticationPrincipal User user) {
        Url url = service.getOriginalUrl(shortLink, user);
        return ResponseEntity.ok(Map.of("clickCount", url.getClickCount()));
    }

    @DeleteMapping("/{shortLink}")
    public ResponseEntity<Map<String, String>> deleteShortLink(
            @PathVariable String shortLink,
            @AuthenticationPrincipal User user) {
        service.deleteShortLink(shortLink, user);
        return ResponseEntity.ok(Map.of("message", "URL deleted successfully"));
    }

    @PatchMapping(value = "/{shortLink}/extend-expiry")
    public ResponseEntity<UrlResponseDto> extendExpiry(
            @PathVariable String shortLink,
            @RequestBody(required = false) ExtendExpiryRequest request,
            @RequestParam(required = false) Integer days,
            @AuthenticationPrincipal User user) {
        int additionalDays = (request != null && request.days() != null)
                ? request.days()
                : (days != null ? days : 30);
        Url url = service.extendExpiration(shortLink, additionalDays, user);
        UrlResponseDto response = new UrlResponseDto(
                url.getOriginalUrl(),
                url.getShortLink(),
                url.getExpirationTime()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-links")
    public ResponseEntity<List<UrlResponseDto>> getMyLinks(@AuthenticationPrincipal User user) {
        List<UrlResponseDto> links = service.getUserLinks(user).stream()
                .map(url -> new UrlResponseDto(
                        url.getOriginalUrl(),
                        url.getShortLink(),
                        url.getExpirationTime()
                ))
                .toList();
        return ResponseEntity.ok(links);
    }
}
