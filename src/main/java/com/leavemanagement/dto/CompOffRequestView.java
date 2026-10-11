package com.leavemanagement.dto;

import com.leavemanagement.entity.LeaveStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Comp-off request as returned by the API, with names and approval history
 */
public record CompOffRequestView(
        Integer compOffRequestId,
        Integer employeeId,
        String employeeName,
        String reportingLeadName,
        LocalDate workedFrom,
        LocalDate workedTo,
        BigDecimal days,
        String reason,
        LeaveStatus status,
        LocalDate requestDate,
        List<ApprovalView> approvals) {
}
