package com.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * LeaveRequest entity class representing a leave request submitted by an employee
 */
@Entity
@Table(name = "leave_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer leaveRequestId;

    @Column(nullable = false)
    private Integer employeeId;

    @Column(nullable = false)
    private Integer leaveTypeId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status;

    @Column(nullable = false)
    private LocalDate requestDate;

    // Set for a half-day leave (start and end date must be the same day)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private HalfDay halfDay;

    /**
     * Calendar days covered, or 0.5 for a half-day leave
     */
    public BigDecimal getNumberOfDays() {
        if (startDate == null || endDate == null) {
            return BigDecimal.ZERO;
        }
        if (halfDay != null) {
            return new BigDecimal("0.5");
        }
        return BigDecimal.valueOf(endDate.toEpochDay() - startDate.toEpochDay() + 1);
    }

    @Override
    public String toString() {
        return "LeaveRequest{" +
                "leaveRequestId=" + leaveRequestId +
                ", employeeId=" + employeeId +
                ", leaveTypeId=" + leaveTypeId +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", reason='" + reason + '\'' +
                ", status='" + status + '\'' +
                ", requestDate=" + requestDate +
                ", halfDay=" + halfDay +
                '}';
    }
}

