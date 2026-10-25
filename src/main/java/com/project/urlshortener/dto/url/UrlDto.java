package com.project.urlshortener.dto.url;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UrlDto(
        @NotBlank(message = "URL must not be blank")
        @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
        String url,
        Integer expirationDays,
        @Size(min = 3, max = 14, message = "Custom short link must be 3-14 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Short link can only contain letters, numbers, hyphens, and underscores")
        String shortLink
) {}
