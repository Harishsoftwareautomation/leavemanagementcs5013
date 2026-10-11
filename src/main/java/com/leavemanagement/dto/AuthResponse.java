package com.leavemanagement.dto;

import com.leavemanagement.entity.Role;
import com.leavemanagement.entity.UserAccount;
import com.leavemanagement.security.JwtService;
import java.time.Instant;

/**
 * Login result for API clients: send the token as "Authorization: Bearer <token>" until expiresAt
 */
public record AuthResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        String username,
        Role role,
        boolean mustChangePassword) {

    public static AuthResponse of(JwtService.IssuedToken token, UserAccount account) {
        return new AuthResponse(token.token(), "Bearer", token.expiresAt(), account.getUsername(), account.getRole(),
                account.isMustChangePassword());
    }
}
