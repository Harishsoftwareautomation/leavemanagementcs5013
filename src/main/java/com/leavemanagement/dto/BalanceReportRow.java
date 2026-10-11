package com.leavemanagement.dto;

import com.leavemanagement.entity.EmployeeStatus;
import java.math.BigDecimal;

/**
 * Leave balance report: one row per employee per leave type.
 * remaining = total + compOffCredited + adjustments - used. Year 0 is the comp-off balance, which has no year.
 */
public record BalanceReportRow(
        Integer employeeId,
        String employeeName,
        String department,
        EmployeeStatus employeeStatus,
        String leadName,
        Integer leaveTypeId,
        String leaveTypeName,
        Integer year,
        BigDecimal totalDays,
        BigDecimal usedDays,
        BigDecimal compOffCredited,
        BigDecimal adjustments,
        BigDecimal remainingDays) {
}
