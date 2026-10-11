package com.leavemanagement.dto;

import java.math.BigDecimal;

/**
 * Sums over all balances of a year; filled by LeaveBalanceRepository (SUM is null when there are no balances)
 */
public record BalanceTotals(BigDecimal allotted, BigDecimal used, BigDecimal remaining) {
    public BalanceTotals {
        allotted = allotted == null ? BigDecimal.ZERO : allotted;
        used = used == null ? BigDecimal.ZERO : used;
        remaining = remaining == null ? BigDecimal.ZERO : remaining;
    }
}
