package com.project.urlshortner.model;



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
public class Url {

    @Id
    @GeneratedValue
    private long id;
    @Column(columnDefinition = "TEXT")
    private String originalUrl;
    @Column(unique = true)
    private String shortLink;
    private LocalDateTime createdTime;
    private LocalDateTime expirationTime;
    private Long clickCount = 0L;

}
