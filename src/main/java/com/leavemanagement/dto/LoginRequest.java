package com.leavemanagement.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API login with email and password
 */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
