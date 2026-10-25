package com.project.urlshortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.transaction.Transactional;

import com.project.urlshortener.model.Url;
import com.project.urlshortener.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByShortLink(String shortLink);

    List<Url> findByUser(User user);

    List<Url> findByExpirationTimeBefore(LocalDateTime time);

    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.shortLink = :shortLink")
    void incrementClickCount(@Param("shortLink") String shortLink);

    @Modifying
    @Transactional
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + :delta WHERE u.shortLink = :shortLink")
    void incrementClickCountByDelta(@Param("shortLink") String shortLink, @Param("delta") long delta);

    @Modifying
    @Transactional
    @Query("DELETE FROM Url u WHERE u.expirationTime < :time")
    int deleteByExpirationTimeBefore(@Param("time") LocalDateTime time);
}
