package com.leavemanagement.security;

import com.leavemanagement.entity.Role;
import com.leavemanagement.entity.UserAccount;
import com.leavemanagement.repository.UserAccountRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = new JwtService("unit-test-secret-of-at-least-32-characters!", 60, false);
    private final UserAccountRepository repository = mock(UserAccountRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, repository);
    private UserAccount account;

    @BeforeEach
    void setUp() {
        account = JwtServiceTest.account(5, 1);
        account.setRole(Role.HR_ADMIN);
        account.setEnabled(true);
        when(repository.findById(5)).thenReturn(Optional.of(account));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse run(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertThat(chain.getRequest()).as("the request always continues down the chain").isNotNull();
        return response;
    }

    private MockHttpServletRequest withCookie(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(JwtService.COOKIE_NAME, token));
        return request;
    }

    @Test
    void validCookieLogsTheUserInWithTheirCurrentRole() throws Exception {
        String token = jwt.issue(account).token();
        account.setRole(Role.EMPLOYEE); // HR changed the role after the token was issued
        run(withCookie(token));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        AppUserDetails user = (AppUserDetails) auth.getPrincipal();
        assertThat(user.getUserId()).isEqualTo(5);
        assertThat(user.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void validBearerHeaderLogsTheUserIn() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt.issue(account).token());
        run(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void disabledAccountIsNotLoggedInAndItsCookieIsCleared() throws Exception {
        String token = jwt.issue(account).token();
        account.setEnabled(false);
        MockHttpServletResponse response = run(withCookie(token));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");
    }

    @Test
    void cancelledTokenIsRejected() throws Exception {
        String token = jwt.issue(account).token();
        account.revokeTokens(); // logout or password change
        run(withCookie(token));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deletedAccountIsRejected() throws Exception {
        String token = jwt.issue(account).token();
        when(repository.findById(5)).thenReturn(Optional.empty());
        run(withCookie(token));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidBearerTokenDoesNotTouchCookies() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer garbage");
        MockHttpServletResponse response = run(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getHeader("Set-Cookie")).isNull();
        verify(repository, never()).findById(any());
    }

    @Test
    void requestWithoutTokenPassesThroughAnonymously() throws Exception {
        MockHttpServletResponse response = run(new MockHttpServletRequest());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getHeader("Set-Cookie")).isNull();
        verifyNoInteractions(repository);
    }
}
