package com.leavemanagement.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Utility class for date operations
 */
public class DateUtil {

    /**
     * Calculate number of working days between two dates (excluding weekends)
     */
    public static int getWorkingDays(LocalDate startDate, LocalDate endDate) {
        int workingDays = 0;
        LocalDate date = startDate;

        while (!date.isAfter(endDate)) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            date = date.plusDays(1);
        }
        return workingDays;
    }

    /**
     * Check if a date is a weekend
     */
    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /**
     * Get current year
     */
    public static int getCurrentYear() {
        return LocalDate.now().getYear();
    }

    /**
     * Check if a date is in the past
     */
    public static boolean isPastDate(LocalDate date) {
        return date.isBefore(LocalDate.now());
    }

    /**
     * Check if a date is today or in the future
     */
    public static boolean isFutureOrToday(LocalDate date) {
        return date.isEqual(LocalDate.now()) || date.isAfter(LocalDate.now());
    }

    /**
     * Get number of days between two dates
     */
    public static int getDaysBetween(LocalDate startDate, LocalDate endDate) {
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }

    /**
     * Get the last day of the year for a given date
     */
    public static LocalDate getLastDayOfYear(LocalDate date) {
        return LocalDate.of(date.getYear(), 12, 31);
    }

    /**
     * Get the first day of the year for a given date
     */
    public static LocalDate getFirstDayOfYear(LocalDate date) {
        return LocalDate.of(date.getYear(), 1, 1);
    }
}
