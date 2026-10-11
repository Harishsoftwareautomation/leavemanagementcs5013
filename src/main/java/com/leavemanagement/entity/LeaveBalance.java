package com.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Leave balance of an employee for one leave type and year.
 * Remaining = total allotted + adjustments (comp-off credits, HR corrections) - used; it may go below zero.
 * The comp-off balance does not belong to a year and is stored with year = 0.
 */
@Entity
@Table(name = "leave_balance", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employeeId", "leaveTypeId", "year"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {
    public static final int NO_YEAR = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer leaveBalanceId;

    @Column(nullable = false)
    private Integer employeeId;

    @Column(nullable = false)
    private Integer leaveTypeId;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal totalDaysAllowed;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal usedDays;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal adjustedDays;

    @Column(nullable = false)
    private Integer year;

    public BigDecimal getRemainingDays() {
        return totalDaysAllowed.add(adjustedDays).subtract(usedDays);
    }

    @Override
    public String toString() {
        return "LeaveBalance{" +
                "leaveBalanceId=" + leaveBalanceId +
                ", employeeId=" + employeeId +
                ", leaveTypeId=" + leaveTypeId +
                ", totalDaysAllowed=" + totalDaysAllowed +
                ", usedDays=" + usedDays +
                ", adjustedDays=" + adjustedDays +
                ", year=" + year +
                '}';
    }
}
