package com.leavemanagement.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilTest {
    // 2026-10-05 is a Monday
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    @Test
    void workingDaysSkipWeekends() {
        assertThat(DateUtil.getWorkingDays(MONDAY, MONDAY.plusDays(6))).isEqualTo(5);  // Mon..Sun
        assertThat(DateUtil.getWorkingDays(MONDAY.plusDays(5), MONDAY.plusDays(6))).isZero(); // Sat..Sun
        assertThat(DateUtil.getWorkingDays(MONDAY, MONDAY)).isEqualTo(1);
    }

    @Test
    void workingDaysOfAnEmptyRangeIsZero() {
        assertThat(DateUtil.getWorkingDays(MONDAY, MONDAY.minusDays(1))).isZero();
    }

    @Test
    void weekendDetection() {
        assertThat(DateUtil.isWeekend(MONDAY.plusDays(5))).isTrue();  // Saturday
        assertThat(DateUtil.isWeekend(MONDAY.plusDays(6))).isTrue();  // Sunday
        assertThat(DateUtil.isWeekend(MONDAY)).isFalse();
        assertThat(DateUtil.isWeekend(MONDAY.plusDays(4))).isFalse(); // Friday
    }

    @Test
    void daysBetweenCountsBothEnds() {
        assertThat(DateUtil.getDaysBetween(MONDAY, MONDAY)).isEqualTo(1);
        assertThat(DateUtil.getDaysBetween(MONDAY, MONDAY.plusDays(9))).isEqualTo(10);
        assertThat(DateUtil.getDaysBetween(LocalDate.of(2024, 2, 28), LocalDate.of(2024, 3, 1))).isEqualTo(3); // leap year
    }

    @Test
    void pastAndFutureRelativeToToday() {
        LocalDate today = LocalDate.now();
        assertThat(DateUtil.isPastDate(today.minusDays(1))).isTrue();
        assertThat(DateUtil.isPastDate(today)).isFalse();
        assertThat(DateUtil.isFutureOrToday(today)).isTrue();
        assertThat(DateUtil.isFutureOrToday(today.plusDays(1))).isTrue();
        assertThat(DateUtil.isFutureOrToday(today.minusDays(1))).isFalse();
    }

    @Test
    void yearBoundaries() {
        assertThat(DateUtil.getFirstDayOfYear(MONDAY)).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(DateUtil.getLastDayOfYear(MONDAY)).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(DateUtil.getCurrentYear()).isEqualTo(LocalDate.now().getYear());
    }
}
