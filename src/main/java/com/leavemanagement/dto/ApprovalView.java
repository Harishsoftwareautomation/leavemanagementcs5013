package com.leavemanagement.dto;

import com.leavemanagement.entity.ApprovalAction;
import com.leavemanagement.entity.ApprovalLevel;
import com.leavemanagement.entity.LeaveApproval;
import java.time.LocalDateTime;

/**
 * One step of a leave request's approval history
 */
public record ApprovalView(
        ApprovalLevel level,
        ApprovalAction action,
        String approverName,
        String comments,
        boolean onBehalf,
        LocalDateTime actedAt) {

    public static ApprovalView of(LeaveApproval a) {
        return new ApprovalView(a.getLevel(), a.getAction(), a.getApproverName(), a.getComments(),
                a.isOnBehalf(), a.getActedAt());
    }
}
