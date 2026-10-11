package com.leavemanagement.security;

import com.leavemanagement.entity.Role;
import com.leavemanagement.entity.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A user with a temporary password can only reach the change-password page, /api/me/** and /api/auth/**
 */
class MustChangePasswordInterceptorTest {
    private final MustChangePasswordInterceptor interceptor = new MustChangePasswordInterceptor();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void loggedIn(boolean mustChangePassword) {
        UserAccount a = new UserAccount();
        a.setUserId(1);
        a.setUsername("asha@test.com");
        a.setPasswordHash("x");
        a.setRole(Role.EMPLOYEE);
        a.setMustChangePassword(mustChangePassword);
        AppUserDetails user = new AppUserDetails(a);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private boolean allowed(String path, MockHttpServletResponse response) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lms" + path);
        request.setContextPath("/lms");
        return interceptor.preHandle(request, response, new Object());
    }

    @Test
    void temporaryPasswordBlocksApiCallsWith403() throws Exception {
        loggedIn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThat(allowed("/api/leave-requests", response)).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void temporaryPasswordRedirectsPagesToChangePassword() throws Exception {
        loggedIn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThat(allowed("/dashboard", response)).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/lms/change-password");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/change-password", "/api/me", "/api/me/password", "/api/auth/logout"})
    void passwordChangeAndLogoutStayAvailable(String path) throws Exception {
        loggedIn(true);
        assertThat(allowed(path, new MockHttpServletResponse())).isTrue();
    }

    @Test
    void usersWithTheirOwnPasswordAreNotAffected() throws Exception {
        loggedIn(false);
        assertThat(allowed("/dashboard", new MockHttpServletResponse())).isTrue();
    }

    @Test
    void anonymousRequestsAreLeftToSpringSecurity() throws Exception {
        assertThat(allowed("/dashboard", new MockHttpServletResponse())).isTrue();
    }
}
