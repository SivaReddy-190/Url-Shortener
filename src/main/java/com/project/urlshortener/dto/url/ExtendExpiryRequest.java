package com.project.urlshortener.dto.url;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExtendExpiryRequest(
        @NotNull(message = "Days must not be null")
        @Min(value = 1, message = "Days must be at least 1")
        Integer days
) {}
