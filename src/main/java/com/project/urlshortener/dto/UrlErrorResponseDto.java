package com.project.urlshortener.dto;

public record UrlErrorResponseDto(
        String status,
        String error
) {}
