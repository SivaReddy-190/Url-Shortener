package com.project.urlshortener.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Size;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UrlDto {

    private String url;
    private Integer expirationDays;
    @Size(min = 3, max = 20)
    private String shortLink;
}
