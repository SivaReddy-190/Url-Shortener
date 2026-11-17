package com.project.urlshortener.util;

/**
 * Lightweight user-agent parser using regex.
 * For production accuracy, consider replacing with the ua_parser or Yauaa library.
 */
public final class UserAgentParser {

    private UserAgentParser() {}

    public record ParsedUserAgent(
            String browserName,
            String browserVersion,
            String osName,
            String osVersion,
            String deviceType
    ) {}

    public static ParsedUserAgent parse(String ua) {
        if (ua == null || ua.isBlank()) {
            return new ParsedUserAgent("unknown", "", "unknown", "", "unknown");
        }

        String browserName = extractBrowser(ua);
        String browserVersion = extractBrowserVersion(ua, browserName);
        String osName = extractOs(ua);
        String osVersion = extractOsVersion(ua, osName);
        String deviceType = detectDeviceType(ua);

        return new ParsedUserAgent(browserName, browserVersion, osName, osVersion, deviceType);
    }

    private static String extractBrowser(String ua) {
        if (ua.contains("Edg/") || ua.contains("Edge/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("Chrome/") && !ua.contains("Chromium")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/") && !ua.contains("Chrome")) return "Safari";
        if (ua.contains("MSIE") || ua.contains("Trident/")) return "IE";
        return "unknown";
    }

    private static String extractBrowserVersion(String ua, String browser) {
        return switch (browser) {
            case "Chrome" -> extractVersion(ua, "Chrome/");
            case "Firefox" -> extractVersion(ua, "Firefox/");
            case "Safari" -> extractVersion(ua, "Version/");
            case "Edge" -> extractVersion(ua, ua.contains("Edg/") ? "Edg/" : "Edge/");
            case "Opera" -> extractVersion(ua, ua.contains("OPR/") ? "OPR/" : "Opera/");
            default -> "";
        };
    }

    private static String extractOs(String ua) {
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")) return "iOS";
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("CrOS")) return "ChromeOS";
        return "unknown";
    }

    private static String extractOsVersion(String ua, String os) {
        return switch (os) {
            case "Windows" -> extractVersion(ua, "Windows NT ");
            case "Android" -> extractVersion(ua, "Android ");
            case "iOS" -> {
                int idx = ua.indexOf("OS ");
                if (idx > 0) {
                    String sub = ua.substring(idx + 3);
                    int end = sub.indexOf(' ');
                    yield end > 0 ? sub.substring(0, end).replace('_', '.') : "";
                }
                yield "";
            }
            case "macOS" -> extractVersion(ua, "Mac OS X ");
            default -> "";
        };
    }

    private static String detectDeviceType(String ua) {
        String lower = ua.toLowerCase();
        if (lower.contains("tablet") || lower.contains("ipad")) return "tablet";
        if (lower.contains("mobile") || lower.contains("iphone") || lower.contains("android")) {
            if (lower.contains("android") && !lower.contains("mobile")) return "tablet";
            return "mobile";
        }
        return "desktop";
    }

    private static String extractVersion(String ua, String prefix) {
        int idx = ua.indexOf(prefix);
        if (idx < 0) return "";
        String sub = ua.substring(idx + prefix.length());
        StringBuilder version = new StringBuilder();
        for (char c : sub.toCharArray()) {
            if (Character.isDigit(c) || c == '.') {
                version.append(c);
            } else {
                break;
            }
        }
        return version.toString();
    }
}
