package com.project.urlshortener.service.impl;

import com.project.urlshortener.dto.analytics.BreakdownEntry;
import com.project.urlshortener.dto.analytics.ClicksSummaryResponse;
import com.project.urlshortener.dto.analytics.DashboardResponse;
import com.project.urlshortener.dto.analytics.TimeSeriesPoint;
import com.project.urlshortener.exception.AccessDeniedException;
import com.project.urlshortener.exception.UrlNotFoundException;
import com.project.urlshortener.model.Url;
import com.project.urlshortener.model.User;
import com.project.urlshortener.repository.ClickEventRepository;
import com.project.urlshortener.repository.UrlRepository;
import com.project.urlshortener.service.AnalyticsDashboardService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AnalyticsDashboardServiceImpl implements AnalyticsDashboardService {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    @Override
    public DashboardResponse getDashboard(String shortLink, int days, User user) {
        Url url = findUrlAndVerifyOwnership(shortLink, user);
        Instant from = daysAgo(days);
        Instant now = Instant.now();

        return new DashboardResponse(
                shortLink,
                url.getOriginalUrl(),
                clickEventRepository.countByShortLink(shortLink),
                clickEventRepository.countByShortLinkAndClickedAtBetween(shortLink, daysAgo(1), now),
                clickEventRepository.countByShortLinkAndClickedAtBetween(shortLink, daysAgo(7), now),
                clickEventRepository.countByShortLinkAndClickedAtBetween(shortLink, daysAgo(30), now),
                mapTimeSeries(clickEventRepository.countByDateSince(shortLink, from)),
                mapBreakdown(clickEventRepository.countByDeviceType(shortLink, from)),
                mapBreakdown(clickEventRepository.countByBrowser(shortLink, from)),
                mapBreakdown(clickEventRepository.countByOs(shortLink, from)),
                mapBreakdown(clickEventRepository.countByCountry(shortLink, from)),
                mapBreakdown(clickEventRepository.topReferrers(shortLink, from, 10))
        );
    }

    @Override
    public ClicksSummaryResponse getClicksSummary(String shortLink, User user) {
        findUrlAndVerifyOwnership(shortLink, user);
        Instant now = Instant.now();
        return new ClicksSummaryResponse(
                shortLink,
                clickEventRepository.countByShortLink(shortLink),
                clickEventRepository.countByShortLinkAndClickedAtBetween(shortLink, daysAgo(1), now),
                clickEventRepository.countByShortLinkAndClickedAtBetween(shortLink, daysAgo(7), now),
                clickEventRepository.countByShortLinkAndClickedAtBetween(shortLink, daysAgo(30), now)
        );
    }

    @Override
    public List<TimeSeriesPoint> getClicksByDate(String shortLink, int days, User user) {
        findUrlAndVerifyOwnership(shortLink, user);
        return mapTimeSeries(clickEventRepository.countByDateSince(shortLink, daysAgo(days)));
    }

    @Override
    public List<TimeSeriesPoint> getClicksByHour(String shortLink, int hours, User user) {
        findUrlAndVerifyOwnership(shortLink, user);
        Instant from = Instant.now().minus(hours, ChronoUnit.HOURS);
        return mapTimeSeries(clickEventRepository.countByHourSince(shortLink, from));
    }

    @Override
    public List<BreakdownEntry> getDeviceBreakdown(String shortLink, int days, User user) {
        findUrlAndVerifyOwnership(shortLink, user);
        return mapBreakdown(clickEventRepository.countByDeviceType(shortLink, daysAgo(days)));
    }

    @Override
    public List<BreakdownEntry> getBrowserBreakdown(String shortLink, int days, User user) {
        findUrlAndVerifyOwnership(shortLink, user);
        return mapBreakdown(clickEventRepository.countByBrowser(shortLink, daysAgo(days)));
    }

    @Override
    public List<BreakdownEntry> getOsBreakdown(String shortLink, int days, User user) {
        findUrlAndVerifyOwnership(shortLink, user);
        return mapBreakdown(clickEventRepository.countByOs(shortLink, daysAgo(days)));
    }

    @Override
    public List<BreakdownEntry> getCountryBreakdown(String shortLink, int days, User user) {
        findUrlAndVerifyOwnership(shortLink, user);
        return mapBreakdown(clickEventRepository.countByCountry(shortLink, daysAgo(days)));
    }

    @Override
    public List<BreakdownEntry> getTopReferrers(String shortLink, int days, int limit, User user) {
        findUrlAndVerifyOwnership(shortLink, user);
        return mapBreakdown(clickEventRepository.topReferrers(shortLink, daysAgo(days), limit));
    }

    // ── Helpers ──

    private Url findUrlAndVerifyOwnership(String shortLink, User user) {
        Url url = urlRepository.findByShortLink(shortLink)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for short link: " + shortLink));
        if (url.getUser() == null || !url.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this link");
        }
        return url;
    }

    private Instant daysAgo(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    private List<TimeSeriesPoint> mapTimeSeries(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new TimeSeriesPoint(row[0].toString(), ((Number) row[1]).longValue()))
                .toList();
    }

    private List<BreakdownEntry> mapBreakdown(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new BreakdownEntry((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
