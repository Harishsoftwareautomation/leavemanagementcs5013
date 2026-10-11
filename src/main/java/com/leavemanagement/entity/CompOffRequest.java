package com.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request to credit compensatory leave for days worked on a weekend or holiday.
 * Whole days only (one per day worked); approved like leave (Team Lead, then HR).
 */
@Entity
@Table(name = "comp_off_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompOffRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer compOffRequestId;

    @Column(nullable = false)
    private Integer employeeId;

    @Column(nullable = false)
    private LocalDate workedFrom;

    @Column(nullable = false)
    private LocalDate workedTo;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal days;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status;

    @Column(nullable = false)
    private LocalDate requestDate;
}
