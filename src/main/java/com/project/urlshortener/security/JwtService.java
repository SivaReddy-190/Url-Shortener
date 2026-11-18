package com.project.urlshortener.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Fail-fast on startup if the JWT secret is missing, uses the insecure default,
     * or is too short. This prevents the application from running with a compromised
     * signing key in production.
     */
    @PostConstruct
    public void validateSecret() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "FATAL: jwt.secret is not configured. Set the JWT_SECRET environment variable.");
        }
        if (secretKey.startsWith("default-dev-secret")) {
            throw new IllegalStateException(
                    "FATAL: jwt.secret is using the insecure default value. " +
                    "Set a strong JWT_SECRET environment variable (min 256 bits / 32 bytes).");
        }
        byte[] keyBytes = decodeKeyBytes(secretKey);
        log.info("JWT secret validation passed (key size: {} bits)", keyBytes.length * 8);
    }

    private byte[] decodeKeyBytes(String secret) {
        // 1. Try standard Base64
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded != null && decoded.length >= 32) {
                return decoded;
            }
        } catch (Exception ignored) {
            // Not standard Base64
        }

        // 2. Try URL-safe Base64 (supports '_' and '-')
        try {
            byte[] decoded = Decoders.BASE64URL.decode(secret);
            if (decoded != null && decoded.length >= 32) {
                return decoded;
            }
        } catch (Exception ignored) {
            // Not Base64URL
        }

        // 3. Fallback: raw UTF-8 string if it is at least 32 characters (256 bits)
        byte[] rawBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (rawBytes.length >= 32) {
            return rawBytes;
        }

        throw new IllegalStateException(
                "FATAL: jwt.secret must be at least 256 bits (32 bytes) when decoded, or a string of at least 32 characters. " +
                "Current byte length: " + rawBytes.length);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(Map.of(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = decodeKeyBytes(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

