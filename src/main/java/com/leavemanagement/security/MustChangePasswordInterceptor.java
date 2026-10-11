package com.leavemanagement.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Sends users with a temporary password to the change-password page until they set their own.
 * Only /change-password, /api/me/** and /api/auth/** stay available meanwhile.
 */
@Component
public class MustChangePasswordInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails user) || !user.isMustChangePassword()) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals("/change-password") || path.startsWith("/api/me") || path.startsWith("/api/auth/")) {
            return true;
        }
        if (path.startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Password change required");
        } else {
            response.sendRedirect(request.getContextPath() + "/change-password");
        }
        return false;
    }
}
