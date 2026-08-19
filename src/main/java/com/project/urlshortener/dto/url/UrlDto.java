package com.project.urlshortener.dto.url;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UrlDto(
        @NotBlank(message = "URL must not be blank")
        String url,
        Integer expirationDays,
        @Size(min = 3, max = 14, message = "Custom short link must be 3-14 Characters")
        String shortLink
) {}
