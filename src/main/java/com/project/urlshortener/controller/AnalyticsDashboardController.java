package com.project.urlshortener.controller;

import com.project.urlshortener.dto.analytics.BreakdownEntry;
import com.project.urlshortener.dto.analytics.ClicksSummaryResponse;
import com.project.urlshortener.dto.analytics.DashboardResponse;
import com.project.urlshortener.dto.analytics.TimeSeriesPoint;
import com.project.urlshortener.model.User;
import com.project.urlshortener.service.AnalyticsDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics Dashboard", description = "Click analytics and dashboard endpoints")
public class AnalyticsDashboardController {

    private final AnalyticsDashboardService dashboardService;

    /**
     * Full dashboard — all breakdowns in a single response.
     */
    @Operation(summary = "Get full analytics dashboard for a short link")
    @GetMapping("/dashboard/{shortLink}")
    public ResponseEntity<DashboardResponse> getDashboard(
            @PathVariable String shortLink,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getDashboard(shortLink, days, user));
    }

    /**
     * Quick click count summary (total, 24h, 7d, 30d).
     */
    @Operation(summary = "Get click count summary")
    @GetMapping("/summary/{shortLink}")
    public ResponseEntity<ClicksSummaryResponse> getClicksSummary(
            @PathVariable String shortLink,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getClicksSummary(shortLink, user));
    }

    /**
     * Time series — clicks grouped by date.
     */
    @Operation(summary = "Get clicks grouped by date")
    @GetMapping("/clicks-by-date/{shortLink}")
    public ResponseEntity<List<TimeSeriesPoint>> getClicksByDate(
            @PathVariable String shortLink,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getClicksByDate(shortLink, days, user));
    }

    /**
     * Time series — clicks grouped by hour (for recent views).
     */
    @Operation(summary = "Get clicks grouped by hour")
    @GetMapping("/clicks-by-hour/{shortLink}")
    public ResponseEntity<List<TimeSeriesPoint>> getClicksByHour(
            @PathVariable String shortLink,
            @RequestParam(defaultValue = "24") int hours,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getClicksByHour(shortLink, hours, user));
    }

    /**
     * Breakdown — device types (desktop, mobile, tablet).
     */
    @Operation(summary = "Get device type breakdown")
    @GetMapping("/devices/{shortLink}")
    public ResponseEntity<List<BreakdownEntry>> getDeviceBreakdown(
            @PathVariable String shortLink,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getDeviceBreakdown(shortLink, days, user));
    }

    /**
     * Breakdown — browsers.
     */
    @Operation(summary = "Get browser breakdown")
    @GetMapping("/browsers/{shortLink}")
    public ResponseEntity<List<BreakdownEntry>> getBrowserBreakdown(
            @PathVariable String shortLink,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getBrowserBreakdown(shortLink, days, user));
    }

    /**
     * Breakdown — operating systems.
     */
    @Operation(summary = "Get OS breakdown")
    @GetMapping("/os/{shortLink}")
    public ResponseEntity<List<BreakdownEntry>> getOsBreakdown(
            @PathVariable String shortLink,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getOsBreakdown(shortLink, days, user));
    }

    /**
     * Breakdown — countries.
     */
    @Operation(summary = "Get country breakdown")
    @GetMapping("/countries/{shortLink}")
    public ResponseEntity<List<BreakdownEntry>> getCountryBreakdown(
            @PathVariable String shortLink,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getCountryBreakdown(shortLink, days, user));
    }

    /**
     * Breakdown — top referrers.
     */
    @Operation(summary = "Get top referrers")
    @GetMapping("/referrers/{shortLink}")
    public ResponseEntity<List<BreakdownEntry>> getTopReferrers(
            @PathVariable String shortLink,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getTopReferrers(shortLink, days, limit, user));
    }
}
