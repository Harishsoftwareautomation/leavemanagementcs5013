package com.leavemanagement.util;

import com.leavemanagement.entity.EmployeeStatus;
import com.leavemanagement.entity.LeaveStatus;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Utility class for validation operations
 */
public class ValidationUtil {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    /**
     * Validate email address
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate phone number (10 digits)
     */
    public static boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.matches("\\d{10}");
    }

    /**
     * Check if string is empty or null
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Validate salary (must be positive)
     */
    public static boolean isValidSalary(double salary) {
        return salary > 0;
    }

    /**
     * Validate date range
     */
    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        return startDate != null && endDate != null && !startDate.isAfter(endDate);
    }

    /**
     * Validate number of days
     */
    public static boolean isValidNumberOfDays(int days) {
        return days > 0;
    }

    /**
     * Validate employee ID
     */
    public static boolean isValidEmployeeId(int employeeId) {
        return employeeId > 0;
    }

    /**
     * Validate an employee status name (ACTIVE, INACTIVE, RESIGNED, RELIEVED)
     */
    public static boolean isValidStatus(String status) {
        if (isEmpty(status)) {
            return false;
        }
        try {
            EmployeeStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validate leave request status
     */
    public static boolean isValidLeaveRequestStatus(String status) {
        if (isEmpty(status)) {
            return false;
        }
        try {
            LeaveStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
