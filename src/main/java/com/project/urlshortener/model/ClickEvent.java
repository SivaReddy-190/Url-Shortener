package com.project.urlshortener.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "click_events", indexes = {
        @Index(name = "idx_click_short_link_time", columnList = "shortLink, clickedAt"),
        @Index(name = "idx_click_time", columnList = "clickedAt"),
        @Index(name = "idx_click_country", columnList = "countryCode, clickedAt")
})
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String shortLink;

    @Column(nullable = false)
    private Instant clickedAt;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 3)
    private String countryCode;

    @Column(length = 128)
    private String city;

    /** "desktop", "mobile", "tablet", "unknown" */
    @Column(length = 16)
    private String deviceType;

    @Column(length = 64)
    private String osName;

    @Column(length = 32)
    private String osVersion;

    @Column(length = 64)
    private String browserName;

    @Column(length = 32)
    private String browserVersion;

    @Column(columnDefinition = "TEXT")
    private String referrerUrl;

    @Column(length = 256)
    private String referrerDomain;

    @Column(columnDefinition = "TEXT")
    private String userAgent;
}
