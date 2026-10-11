package com.leavemanagement.dto;

import com.leavemanagement.entity.EmployeeStatus;
import java.math.BigDecimal;

/**
 * One leave balance joined with its employee, lead and leave type; filled by the report queries in LeaveBalanceRepository
 */
public record BalanceSnapshot(
        Integer employeeId,
        String firstName,
        String lastName,
        String department,
        EmployeeStatus employeeStatus,
        String leadFirstName,
        String leadLastName,
        Integer leaveTypeId,
        String leaveTypeName,
        Integer year,
        BigDecimal totalDaysAllowed,
        BigDecimal usedDays,
        BigDecimal adjustedDays) {

    public String employeeName() {
        return firstName + " " + lastName;
    }

    // Null when the employee has no reporting lead
    public String leadName() {
        return leadFirstName == null ? null : leadFirstName + " " + leadLastName;
    }

    public BigDecimal remainingDays() {
        return totalDaysAllowed.add(adjustedDays).subtract(usedDays);
    }
}
