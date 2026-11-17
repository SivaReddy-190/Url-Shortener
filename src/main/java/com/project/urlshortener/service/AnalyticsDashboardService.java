package com.project.urlshortener.service;

import com.project.urlshortener.dto.analytics.BreakdownEntry;
import com.project.urlshortener.dto.analytics.ClicksSummaryResponse;
import com.project.urlshortener.dto.analytics.DashboardResponse;
import com.project.urlshortener.dto.analytics.TimeSeriesPoint;
import com.project.urlshortener.model.User;

import java.util.List;

/**
 * Provides aggregated analytics data for dashboard views.
 */
public interface AnalyticsDashboardService {

    /** Full dashboard with all breakdowns for a short link. */
    DashboardResponse getDashboard(String shortLink, int days, User user);

    /** Quick click count summary. */
    ClicksSummaryResponse getClicksSummary(String shortLink, User user);

    /** Clicks grouped by date for a given period. */
    List<TimeSeriesPoint> getClicksByDate(String shortLink, int days, User user);

    /** Clicks grouped by hour (for recent periods like last 24h/48h). */
    List<TimeSeriesPoint> getClicksByHour(String shortLink, int hours, User user);

    /** Device type breakdown. */
    List<BreakdownEntry> getDeviceBreakdown(String shortLink, int days, User user);

    /** Browser breakdown. */
    List<BreakdownEntry> getBrowserBreakdown(String shortLink, int days, User user);

    /** OS breakdown. */
    List<BreakdownEntry> getOsBreakdown(String shortLink, int days, User user);

    /** Country breakdown. */
    List<BreakdownEntry> getCountryBreakdown(String shortLink, int days, User user);

    /** Top referrers. */
    List<BreakdownEntry> getTopReferrers(String shortLink, int days, int limit, User user);
}
