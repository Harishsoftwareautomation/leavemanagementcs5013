package com.leavemanagement.dto;

/**
 * Result of creating an employee: the temporary password is shown to HR once and must be changed at first login
 */
public record CreatedEmployeeResponse(EmployeeView employee, String username, String temporaryPassword) {
}
