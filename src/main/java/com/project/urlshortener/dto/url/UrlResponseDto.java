package com.project.urlshortener.dto.url;

import java.time.LocalDateTime;

public record UrlResponseDto(
        String originalUrl,
        String shortLink,
        LocalDateTime expirationTime
) {}
