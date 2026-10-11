package com.leavemanagement.security;

import com.leavemanagement.entity.Role;
import com.leavemanagement.entity.UserAccount;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-of-at-least-32-characters!";
    private final JwtService jwt = new JwtService(SECRET, 60, false);

    static UserAccount account(int userId, int tokenVersion) {
        UserAccount a = new UserAccount();
        a.setUserId(userId);
        a.setUsername("asha@test.com");
        a.setRole(Role.TEAM_LEAD);
        for (int i = 0; i < tokenVersion; i++) {
            a.revokeTokens();
        }
        return a;
    }

    @Test
    void issuedTokenCarriesUserIdAndTokenVersion() {
        JwtService.IssuedToken token = jwt.issue(account(7, 3));
        assertThat(token.token().split("\\.")).hasSize(3); // header.payload.signature
        JwtService.TokenClaims claims = jwt.parse(token.token()).orElseThrow();
        assertThat(claims.userId()).isEqualTo(7);
        assertThat(claims.tokenVersion()).isEqualTo(3);
    }

    @Test
    void tokenExpiresAfterTheConfiguredLifetime() {
        Instant before = Instant.now();
        JwtService.IssuedToken token = jwt.issue(account(1, 0));
        assertThat(Duration.between(before, token.expiresAt()).toMinutes()).isBetween(59L, 60L);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expired = new JwtService(SECRET, -1, false);
        String token = expired.issue(account(1, 0)).token();
        assertThat(jwt.parse(token)).isEmpty();
    }

    @Test
    void alteredTokenIsRejected() {
        String token = jwt.issue(account(1, 0)).token();
        String[] parts = token.split("\\.");
        // Swap the payload for another user's payload: the signature no longer matches
        String otherPayload = jwt.issue(account(2, 0)).token().split("\\.")[1];
        assertThat(jwt.parse(parts[0] + "." + otherPayload + "." + parts[2])).isEmpty();
        assertThat(jwt.parse(token.substring(0, token.length() - 2) + "xx")).isEmpty();
        assertThat(jwt.parse("not-a-token")).isEmpty();
        assertThat(jwt.parse("")).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService other = new JwtService("another-secret-that-is-also-32-chars-long", 60, false);
        assertThat(jwt.parse(other.issue(account(1, 0)).token())).isEmpty();
    }

    @Test
    void withoutASecretEachInstanceUsesItsOwnRandomKey() {
        JwtService a = new JwtService("", 60, false);
        JwtService b = new JwtService(null, 60, false);
        String token = a.issue(account(1, 0)).token();
        assertThat(a.parse(token)).isPresent();
        assertThat(b.parse(token)).isEmpty();
    }

    @Test
    void shortSecretIsRefused() {
        assertThatThrownBy(() -> new JwtService("too-short", 60, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    void tokenIsReadFromBearerHeaderFirstThenCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(jwt.resolveToken(request)).isEmpty();

        request.setCookies(new Cookie(JwtService.COOKIE_NAME, "from-cookie"));
        assertThat(jwt.resolveToken(request)).contains("from-cookie");

        request.addHeader("Authorization", "Bearer from-header");
        assertThat(jwt.resolveToken(request)).contains("from-header");
        assertThat(JwtService.hasBearerHeader(request)).isTrue();
    }

    @Test
    void otherAuthorizationSchemesAreIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        assertThat(jwt.resolveToken(request)).isEmpty();
        assertThat(JwtService.hasBearerHeader(request)).isFalse();
    }

    @Test
    void emptyCookieIsIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(JwtService.COOKIE_NAME, ""));
        assertThat(jwt.resolveToken(request)).isEmpty();
    }

    @Test
    void loginCookieIsHttpOnlyLaxAndScopedToTheApp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/lms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        jwt.addCookie(request, response, jwt.issue(account(1, 0)));

        String header = response.getHeader("Set-Cookie");
        assertThat(header).startsWith(JwtService.COOKIE_NAME + "=ey")
                .contains("Path=/lms", "Max-Age=3600", "HttpOnly", "SameSite=Lax")
                .doesNotContain("Secure");
    }

    @Test
    void secureFlagCanBeSwitchedOnForHttps() {
        JwtService https = new JwtService(SECRET, 60, true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        https.addCookie(new MockHttpServletRequest(), response, https.issue(account(1, 0)));
        assertThat(response.getHeader("Set-Cookie")).contains("Secure", "Path=/");
    }

    @Test
    void clearingTheCookieExpiresIt() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        jwt.clearCookie(new MockHttpServletRequest(), response);
        assertThat(response.getHeader("Set-Cookie")).startsWith(JwtService.COOKIE_NAME + "=;").contains("Max-Age=0");
    }
}
