package com.project.urlshortener.dto.url;

public record UrlErrorResponseDto(
        String status,
        String error
) {}
