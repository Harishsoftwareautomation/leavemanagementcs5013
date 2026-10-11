package com.leavemanagement.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Fields an employee may change on their own profile
 */
public record UpdateProfileRequest(
        @Pattern(regexp = "^$|^\\+?[0-9 -]{7,20}$", message = "must be a valid phone number") String phone) {
}
