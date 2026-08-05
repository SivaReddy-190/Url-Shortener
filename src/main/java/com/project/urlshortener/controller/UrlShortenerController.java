package com.project.urlshortener.controller;

import com.project.urlshortener.dto.UrlDto;
import com.project.urlshortener.dto.UrlResponseDto;
import com.project.urlshortener.model.Url;
import com.project.urlshortener.service.UrlService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RequiredArgsConstructor
@RestController
public class UrlShortenerController {

    private final UrlService service;

    @PostMapping("/generate")
    public ResponseEntity<UrlResponseDto> generateShortLink(@Valid @RequestBody UrlDto urlDto) {
        Url url = service.generateShortLink(urlDto);
        UrlResponseDto response = new UrlResponseDto(
                url.getOriginalUrl(),
                url.getShortLink(),
                url.getExpirationTime()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortLink}")
    public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable String shortLink) {
        Url url = service.getEncodedUrl(shortLink);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url.getOriginalUrl()));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/stats/{shortLink}")
    public ResponseEntity<Map<String, Long>> getStats(@PathVariable String shortLink) {
        Url url = service.getOriginalUrl(shortLink);
        return ResponseEntity.ok(Map.of("clickCount", url.getClickCount()));
    }

    @DeleteMapping("/{shortLink}")
    public ResponseEntity<Map<String, String>> deleteShortLink(@PathVariable String shortLink) {
        service.deleteShortLink(shortLink);
        return ResponseEntity.ok(Map.of("message", "URL deleted successfully"));
    }
}
