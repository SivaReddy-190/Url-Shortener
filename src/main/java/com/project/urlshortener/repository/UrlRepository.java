package com.project.urlshortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.transaction.Transactional;

import com.project.urlshortener.model.Url;

import java.time.LocalDateTime;
import java.util.Optional;


public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByShortLink(String shortLink);

    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.shortLink = :shortLink")
    void incrementClickCount(@Param("shortLink") String shortLink);

    @Modifying
    @Transactional
    @Query("DELETE FROM Url u WHERE u.expirationTime < :time")
    int deleteByExpirationTimeBefore(@Param("time") LocalDateTime time);
}
