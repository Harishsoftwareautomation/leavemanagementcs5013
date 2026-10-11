package com.leavemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * HR balance correction: positive days add to the balance, negative days take away.
 * The year is ignored for the comp-off balance, which does not belong to a year.
 */
public record AdjustmentRequest(
        @NotNull Integer employeeId,
        @NotNull Integer leaveTypeId,
        @NotNull Integer year,
        @NotNull BigDecimal days,
        @NotBlank String reason) {
}
