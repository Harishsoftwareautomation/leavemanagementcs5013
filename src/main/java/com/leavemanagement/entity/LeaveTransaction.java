package com.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ledger entry: one change to an employee's leave balance (positive adds days, negative takes them)
 */
@Entity
@Table(name = "leave_transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer transactionId;

    @Column(nullable = false)
    private Integer employeeId;

    @Column(nullable = false)
    private Integer leaveTypeId;

    // Balance year; 0 for the comp-off balance, which does not belong to a year
    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal days;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    // ID of the leave or comp-off request behind this change, if any
    private Integer referenceId;

    @Column(length = 500)
    private String reason;

    private Integer createdByUserId;

    @Column(nullable = false)
    private String createdByName;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
