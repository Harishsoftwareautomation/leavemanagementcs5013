package com.leavemanagement.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Calculations kept on the entities themselves
 */
class EntityRulesTest {

    @Test
    void leaveDaysAreCalendarDaysIncludingBothEnds() {
        LeaveRequest r = new LeaveRequest();
        r.setStartDate(LocalDate.of(2026, 3, 2));
        r.setEndDate(LocalDate.of(2026, 3, 6));
        assertThat(r.getNumberOfDays()).isEqualByComparingTo("5");
        r.setEndDate(r.getStartDate());
        assertThat(r.getNumberOfDays()).isEqualByComparingTo("1");
    }

    @Test
    void halfDayLeaveCountsAsHalf() {
        LeaveRequest r = new LeaveRequest();
        r.setStartDate(LocalDate.of(2026, 3, 2));
        r.setEndDate(LocalDate.of(2026, 3, 2));
        r.setHalfDay(HalfDay.SECOND_HALF);
        assertThat(r.getNumberOfDays()).isEqualByComparingTo("0.5");
    }

    @Test
    void leaveWithoutDatesHasZeroDays() {
        assertThat(new LeaveRequest().getNumberOfDays()).isEqualByComparingTo("0");
    }

    @Test
    void remainingIsAllottedPlusAdjustmentsMinusUsedAndMayBeNegative() {
        LeaveBalance b = new LeaveBalance(null, 1, 1, new BigDecimal("12"), new BigDecimal("3.5"), new BigDecimal("2"), 2026);
        assertThat(b.getRemainingDays()).isEqualByComparingTo("10.5");
        b.setUsedDays(new BigDecimal("15"));
        assertThat(b.getRemainingDays()).isEqualByComparingTo("-1");
        b.setAdjustedDays(new BigDecimal("-1.5"));
        assertThat(b.getRemainingDays()).isEqualByComparingTo("-4.5");
    }

    @Test
    void onlyTheTwoWaitingStatusesArePending() {
        assertThat(LeaveStatus.PENDING_TL.isPending()).isTrue();
        assertThat(LeaveStatus.PENDING_HR.isPending()).isTrue();
        assertThat(LeaveStatus.APPROVED.isPending()).isFalse();
        assertThat(LeaveStatus.REJECTED.isPending()).isFalse();
        assertThat(LeaveStatus.CANCELLED.isPending()).isFalse();
    }

    @Test
    void fullName() {
        Employee e = new Employee();
        e.setFirstName("Asha");
        e.setLastName("Rao");
        assertThat(e.getFullName()).isEqualTo("Asha Rao");
    }

    @Test
    void revokingTokensRaisesTheTokenVersion() {
        UserAccount a = new UserAccount();
        assertThat(a.getTokenVersion()).isZero();
        a.revokeTokens();
        a.revokeTokens();
        assertThat(a.getTokenVersion()).isEqualTo(2);
    }

    @Test
    void newAccountsMustChangePasswordAndAreEnabled() {
        UserAccount a = new UserAccount();
        assertThat(a.isMustChangePassword()).isTrue();
        assertThat(a.isEnabled()).isTrue();
    }
}
