package com.leavemanagement.controller;

import com.leavemanagement.dto.AuthResponse;
import com.leavemanagement.dto.LoginRequest;
import com.leavemanagement.entity.UserAccount;
import com.leavemanagement.repository.UserAccountRepository;
import com.leavemanagement.security.CurrentUser;
import com.leavemanagement.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Token login for API clients (mobile app, separate frontend, scripts). Browsers use the /login form instead,
 * which puts the same token in an HttpOnly cookie.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;

    /**
     * Email + password -> token. A temporary password still logs in, but only /api/me/** works until it is changed.
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserAccount account = userAccountRepository.findByUsernameIgnoreCase(request.username().trim())
                .filter(UserAccount::isEnabled)
                .filter(a -> passwordEncoder.matches(request.password(), a.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return AuthResponse.of(jwtService.issue(account), account);
    }

    /**
     * Cancels all of the user's tokens (every browser and device)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        userAccountRepository.findById(currentUser.get().getUserId()).ifPresent(account -> {
            account.revokeTokens();
            userAccountRepository.save(account);
        });
        jwtService.clearCookie(request, response);
        return ResponseEntity.noContent().build();
    }
}
