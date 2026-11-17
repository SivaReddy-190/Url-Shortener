package com.project.urlshortener.repository;

import com.project.urlshortener.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortLink(String shortLink);

    long countByShortLinkAndClickedAtBetween(String shortLink, Instant from, Instant to);

    // Clicks grouped by date
    @Query(value = """
            SELECT CAST(clicked_at AS DATE) AS click_date, COUNT(*) AS clicks
            FROM click_events
            WHERE short_link = :shortLink AND clicked_at >= :from
            GROUP BY CAST(clicked_at AS DATE)
            ORDER BY click_date
            """, nativeQuery = true)
    List<Object[]> countByDateSince(@Param("shortLink") String shortLink,
                                    @Param("from") Instant from);

    // Clicks grouped by hour
    @Query(value = """
            SELECT DATE_TRUNC('hour', clicked_at) AS click_hour, COUNT(*) AS clicks
            FROM click_events
            WHERE short_link = :shortLink AND clicked_at >= :from
            GROUP BY DATE_TRUNC('hour', clicked_at)
            ORDER BY click_hour
            """, nativeQuery = true)
    List<Object[]> countByHourSince(@Param("shortLink") String shortLink,
                                    @Param("from") Instant from);

    // Device type breakdown
    @Query(value = """
            SELECT COALESCE(device_type, 'unknown') AS device, COUNT(*) AS clicks
            FROM click_events
            WHERE short_link = :shortLink AND clicked_at >= :from
            GROUP BY device_type
            ORDER BY clicks DESC
            """, nativeQuery = true)
    List<Object[]> countByDeviceType(@Param("shortLink") String shortLink,
                                     @Param("from") Instant from);

    // Browser breakdown
    @Query(value = """
            SELECT COALESCE(browser_name, 'unknown') AS browser, COUNT(*) AS clicks
            FROM click_events
            WHERE short_link = :shortLink AND clicked_at >= :from
            GROUP BY browser_name
            ORDER BY clicks DESC
            """, nativeQuery = true)
    List<Object[]> countByBrowser(@Param("shortLink") String shortLink,
                                   @Param("from") Instant from);

    // OS breakdown
    @Query(value = """
            SELECT COALESCE(os_name, 'unknown') AS os, COUNT(*) AS clicks
            FROM click_events
            WHERE short_link = :shortLink AND clicked_at >= :from
            GROUP BY os_name
            ORDER BY clicks DESC
            """, nativeQuery = true)
    List<Object[]> countByOs(@Param("shortLink") String shortLink,
                              @Param("from") Instant from);

    // Country breakdown
    @Query(value = """
            SELECT COALESCE(country_code, 'XX') AS country, COUNT(*) AS clicks
            FROM click_events
            WHERE short_link = :shortLink AND clicked_at >= :from
            GROUP BY country_code
            ORDER BY clicks DESC
            """, nativeQuery = true)
    List<Object[]> countByCountry(@Param("shortLink") String shortLink,
                                   @Param("from") Instant from);

    // Top referrers
    @Query(value = """
            SELECT COALESCE(referrer_domain, 'direct') AS referrer, COUNT(*) AS clicks
            FROM click_events
            WHERE short_link = :shortLink AND clicked_at >= :from
            GROUP BY referrer_domain
            ORDER BY clicks DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topReferrers(@Param("shortLink") String shortLink,
                                 @Param("from") Instant from,
                                 @Param("limit") int limit);
}
