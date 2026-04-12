package com.project.urlshortner.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UrlDto {

    private String url;
    private Integer expirationDays;
    private String shortLink;
}
