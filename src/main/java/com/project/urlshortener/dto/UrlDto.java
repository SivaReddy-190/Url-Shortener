package com.project.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UrlDto(
        @NotBlank(message = "URL must not be blank")
        String url,
        Integer expirationDays,
        @Size(min = 3, max = 20, message = "Custom short link must be 3-20 characters")
        String shortLink
) {}
