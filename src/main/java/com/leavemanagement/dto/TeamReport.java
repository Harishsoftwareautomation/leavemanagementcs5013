package com.leavemanagement.dto;

import java.util.List;

/**
 * Team Lead's "My team" view: their reports' balances and how many of their requests are waiting.
 * Pending counts include leave and comp-off requests.
 */
public record TeamReport(
        Integer year,
        int teamSize,
        long waitingForMe,
        long waitingForHr,
        List<BalanceReportRow> balances) {
}
