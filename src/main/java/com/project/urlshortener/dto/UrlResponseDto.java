package com.project.urlshortener.dto;

import java.time.LocalDateTime;

public record UrlResponseDto(
        String originalUrl,
        String shortLink,
        LocalDateTime expirationTime
) {}
