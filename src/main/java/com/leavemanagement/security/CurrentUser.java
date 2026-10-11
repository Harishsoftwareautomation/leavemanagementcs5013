package com.leavemanagement.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Optional;

/**
 * Access to the currently logged-in user
 */
@Component
public class CurrentUser {

    public AppUserDetails get() {
        return find().orElseThrow(() -> new AccessDeniedException("Not logged in"));
    }

    /**
     * The logged-in user, or empty when running outside a request (e.g. at startup)
     */
    public Optional<AppUserDetails> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * Employee ID of the current user; fails for accounts without an employee record (the initial admin)
     */
    public Integer requireEmployeeId() {
        Integer employeeId = get().getEmployeeId();
        if (employeeId == null) {
            throw new IllegalArgumentException("This account is not linked to an employee record");
        }
        return employeeId;
    }
}
