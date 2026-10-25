package com.project.urlshortener.model;



import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString(exclude = "user")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(indexes = @Index(name = "idx_short_link", columnList = "shortLink"))
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @EqualsAndHashCode.Include
    private Long id;
    @Column(columnDefinition = "TEXT")
    private String originalUrl;
    @Column(unique = true)
    private String shortLink;
    private LocalDateTime createdTime;
    private LocalDateTime expirationTime;
    @Builder.Default
    private Long clickCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

}
