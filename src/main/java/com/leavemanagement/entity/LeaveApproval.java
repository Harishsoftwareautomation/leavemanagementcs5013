package com.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * One approve/reject action on a leave or comp-off request, at the Team Lead or HR step
 */
@Entity
@Table(name = "leave_approval")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer approvalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestType requestType;

    // ID of the leave request or comp-off request
    @Column(nullable = false)
    private Integer requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalAction action;

    private Integer approverUserId;

    // Null for the initial admin account, which has no employee record
    private Integer approverEmployeeId;

    // Name at the time of the action, so the history stays readable if records change
    @Column(nullable = false)
    private String approverName;

    @Column(length = 500)
    private String comments;

    // True when HR completed the Team Lead step because the lead was away
    @Column(nullable = false)
    private boolean onBehalf;

    @Column(nullable = false)
    private LocalDateTime actedAt;
}
