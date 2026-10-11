package com.leavemanagement.dto;

import com.leavemanagement.entity.Role;

/**
 * The logged-in user's account and, when linked, their employee profile
 */
public record MeResponse(Integer userId, String username, Role role, boolean mustChangePassword, EmployeeView employee) {
}
