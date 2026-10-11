package com.leavemanagement.security;

import com.leavemanagement.entity.UserAccount;
import com.leavemanagement.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Logs the request in from its JWT (Bearer header or login cookie). The account is loaded from the database on
 * every request, so HR's changes apply immediately: a disabled account or a cancelled token (logout, password
 * change or reset) is rejected, and a changed role or password-change flag takes effect on the next request.
 * Registered in SecurityConfig before the logout filter.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final SecurityContextRepository contextRepository = new RequestAttributeSecurityContextRepository();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<String> token = jwtService.resolveToken(request);
        if (token.isPresent()) {
            Optional<UserAccount> account = jwtService.parse(token.get())
                    .flatMap(claims -> userAccountRepository.findById(claims.userId())
                            .filter(UserAccount::isEnabled)
                            .filter(a -> claims.tokenVersion() != null && a.getTokenVersion() == claims.tokenVersion()));
            if (account.isPresent()) {
                AppUserDetails user = new AppUserDetails(account.get());
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                SecurityContextHolder.setContext(context);
                // Marks the request as already logged in, so Spring does not treat it as a new login
                // (which would issue a new CSRF token on every request and break forms open in other tabs)
                contextRepository.saveContext(context, request, response);
            } else if (!JwtService.hasBearerHeader(request)) {
                // Expired, altered or cancelled cookie: remove it so the browser goes back to the login page
                jwtService.clearCookie(request, response);
            }
        }
        chain.doFilter(request, response);
    }
}
