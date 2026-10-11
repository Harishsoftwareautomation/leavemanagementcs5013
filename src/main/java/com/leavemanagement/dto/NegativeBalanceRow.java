package com.leavemanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A balance below zero. negativeSince is the day the ledger last crossed below zero; null if the ledger does not show it.
 */
public record NegativeBalanceRow(
        Integer employeeId,
        String employeeName,
        String department,
        String leadName,
        Integer leaveTypeId,
        String leaveTypeName,
        Integer year,
        BigDecimal remainingDays,
        LocalDate negativeSince) {
}
