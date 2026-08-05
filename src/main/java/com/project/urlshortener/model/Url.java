package com.project.urlshortener.model;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(indexes = @Index(name = "idx_short_link", columnList = "shortLink"))
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Column(columnDefinition = "TEXT")
    private String originalUrl;
    @Column(unique = true)
    private String shortLink;
    private LocalDateTime createdTime;
    private LocalDateTime expirationTime;
    @Builder.Default
    private Long clickCount = 0L;

}
