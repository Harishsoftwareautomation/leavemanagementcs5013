package com.leavemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Employee request for comp-off: the day(s) worked on a weekend or holiday
 */
public record CompOffSubmitRequest(
        @NotNull LocalDate workedFrom,
        @NotNull LocalDate workedTo,
        @NotBlank String reason) {
}
