package com.project.urlshortener.dto;

public record ApiErrorResponse(
        String status,
        String error
) {}
