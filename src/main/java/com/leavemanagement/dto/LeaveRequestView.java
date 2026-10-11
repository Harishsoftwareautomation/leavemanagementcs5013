package com.leavemanagement.dto;

import com.leavemanagement.entity.HalfDay;
import com.leavemanagement.entity.LeaveStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Leave request as returned by the API, with names, approval history and, while pending,
 * the balance the employee would have left after approval (negative if it would go below zero)
 */
public record LeaveRequestView(
        Integer leaveRequestId,
        Integer employeeId,
        String employeeName,
        String reportingLeadName,
        Integer leaveTypeId,
        String leaveTypeName,
        LocalDate startDate,
        LocalDate endDate,
        HalfDay halfDay,
        BigDecimal numberOfDays,
        String reason,
        LeaveStatus status,
        LocalDate requestDate,
        BigDecimal balanceAfterApproval,
        List<ApprovalView> approvals) {
}
