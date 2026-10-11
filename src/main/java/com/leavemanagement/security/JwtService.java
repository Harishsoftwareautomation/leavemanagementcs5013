package com.leavemanagement.security;

import com.leavemanagement.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and checks login tokens (JWT, HMAC-SHA256). The token holds the user ID and the account's token version;
 * role, enabled state and the password-change flag are always read from the database (JwtAuthenticationFilter).
 * Browsers get the token in an HttpOnly cookie; other clients send it as "Authorization: Bearer <token>".
 */
@Slf4j
@Component
public class JwtService {
    public static final String COOKIE_NAME = "LMS_TOKEN";
    private static final String VERSION_CLAIM = "ver";

    private final SecretKey key;
    private final Duration lifetime;
    private final boolean secureCookie;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expiration-minutes:480}") long expirationMinutes,
                      @Value("${app.jwt.cookie-secure:false}") boolean secureCookie) {
        this.key = Keys.hmacShaKeyFor(secretBytes(secret));
        this.lifetime = Duration.ofMinutes(expirationMinutes);
        this.secureCookie = secureCookie;
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public IssuedToken issue(UserAccount account) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lifetime);
        String token = Jwts.builder()
                .subject(String.valueOf(account.getUserId()))
                .claim(VERSION_CLAIM, account.getTokenVersion())
                // For clients' convenience only; the server uses the role stored in the database
                .claim("username", account.getUsername())
                .claim("role", account.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /**
     * User ID and token version from a valid, unexpired token; empty if the token is missing, altered or expired
     */
    public Optional<TokenClaims> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new TokenClaims(Integer.valueOf(claims.getSubject()),
                    claims.get(VERSION_CLAIM, Integer.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record TokenClaims(Integer userId, Integer tokenVersion) {
    }

    /**
     * Token from the Authorization header, or else from the login cookie
     */
    public Optional<String> resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7).trim());
        }
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    return Optional.of(cookie.getValue());
                }
            }
        }
        return Optional.empty();
    }

    public static boolean hasBearerHeader(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null && header.startsWith("Bearer ");
    }

    public void addCookie(HttpServletRequest request, HttpServletResponse response, IssuedToken token) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(request, token.token(), lifetime).toString());
    }

    public void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(request, "", Duration.ZERO).toString());
    }

    private ResponseCookie cookie(HttpServletRequest request, String value, Duration maxAge) {
        String path = request.getContextPath().isEmpty() ? "/" : request.getContextPath();
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    // A configured secret must be at least 32 characters; without one, a random key is used (logins end on restart)
    private static byte[] secretBytes(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("app.jwt.secret (LMS_JWT_SECRET) is not set: using a random key, so everyone must log in again "
                    + "after a restart. Set it to a random string of 32+ characters to keep logins across restarts.");
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            return random;
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret (LMS_JWT_SECRET) must be at least 32 characters");
        }
        return bytes;
    }
}
