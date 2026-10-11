package com.leavemanagement.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {"jane@example.com", "j.doe+leave@company.co.in", "a_b-c@x.org"})
    void validEmails(String email) {
        assertThat(ValidationUtil.isValidEmail(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "plainaddress", "@example.com", "jane@", "jane doe@example.com"})
    void invalidEmails(String email) {
        assertThat(ValidationUtil.isValidEmail(email)).isFalse();
    }

    @Test
    void nullEmailIsInvalid() {
        assertThat(ValidationUtil.isValidEmail(null)).isFalse();
    }

    @Test
    void phoneNumberMustBeTenDigits() {
        assertThat(ValidationUtil.isValidPhoneNumber("9876543210")).isTrue();
        assertThat(ValidationUtil.isValidPhoneNumber("987654321")).isFalse();
        assertThat(ValidationUtil.isValidPhoneNumber("98765432100")).isFalse();
        assertThat(ValidationUtil.isValidPhoneNumber("98765abcde")).isFalse();
        assertThat(ValidationUtil.isValidPhoneNumber(null)).isFalse();
    }

    @Test
    void emptyMeansNullOrBlank() {
        assertThat(ValidationUtil.isEmpty(null)).isTrue();
        assertThat(ValidationUtil.isEmpty("")).isTrue();
        assertThat(ValidationUtil.isEmpty("   ")).isTrue();
        assertThat(ValidationUtil.isEmpty(" x ")).isFalse();
    }

    @Test
    void numericRules() {
        assertThat(ValidationUtil.isValidSalary(1)).isTrue();
        assertThat(ValidationUtil.isValidSalary(0)).isFalse();
        assertThat(ValidationUtil.isValidSalary(-5)).isFalse();
        assertThat(ValidationUtil.isValidNumberOfDays(1)).isTrue();
        assertThat(ValidationUtil.isValidNumberOfDays(0)).isFalse();
        assertThat(ValidationUtil.isValidEmployeeId(1)).isTrue();
        assertThat(ValidationUtil.isValidEmployeeId(0)).isFalse();
    }

    @Test
    void dateRange() {
        LocalDate d = LocalDate.of(2026, 5, 1);
        assertThat(ValidationUtil.isValidDateRange(d, d)).isTrue();
        assertThat(ValidationUtil.isValidDateRange(d, d.plusDays(1))).isTrue();
        assertThat(ValidationUtil.isValidDateRange(d.plusDays(1), d)).isFalse();
        assertThat(ValidationUtil.isValidDateRange(null, d)).isFalse();
        assertThat(ValidationUtil.isValidDateRange(d, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "INACTIVE", "RESIGNED", "RELIEVED"})
    void everyEmployeeStatusIsValid(String status) {
        assertThat(ValidationUtil.isValidStatus(status)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ON_LEAVE", "active", "FIRED"})
    void unknownEmployeeStatusIsInvalid(String status) {
        assertThat(ValidationUtil.isValidStatus(status)).isFalse();
    }

    @Test
    void leaveRequestStatus() {
        for (String s : new String[]{"PENDING_TL", "PENDING_HR", "APPROVED", "REJECTED", "CANCELLED"}) {
            assertThat(ValidationUtil.isValidLeaveRequestStatus(s)).as(s).isTrue();
        }
        assertThat(ValidationUtil.isValidLeaveRequestStatus("PENDING")).isFalse();
        assertThat(ValidationUtil.isValidLeaveRequestStatus(null)).isFalse();
    }
}
