package com.leavemanagement.service;

import com.leavemanagement.entity.LeaveBalance;
import com.leavemanagement.entity.LeaveTransaction;
import com.leavemanagement.entity.LeaveType;
import com.leavemanagement.entity.TransactionType;
import com.leavemanagement.repository.EmployeeRepository;
import com.leavemanagement.repository.LeaveBalanceRepository;
import com.leavemanagement.repository.LeaveTransactionRepository;
import com.leavemanagement.repository.LeaveTypeRepository;
import com.leavemanagement.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Balance rules and the ledger: every change writes exactly one ledger entry
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeaveServiceTest {
    static final int EMP = 1, CASUAL = 10, COMP_OFF = 20, YEAR = 2026;

    @Mock LeaveBalanceRepository balances;
    @Mock LeaveTransactionRepository ledger;
    @Mock LeaveTypeRepository types;
    @Mock EmployeeRepository employees;
    @Mock CurrentUser currentUser;
    @InjectMocks LeaveService service;

    @BeforeEach
    void setUp() {
        when(types.findById(CASUAL)).thenReturn(Optional.of(new LeaveType(CASUAL, "Casual Leave", 12, null, null)));
        LeaveType compOff = new LeaveType(COMP_OFF, "Compensatory Leave", 0, null, "COMP_OFF");
        when(types.findById(COMP_OFF)).thenReturn(Optional.of(compOff));
        when(types.findBySystemCode("COMP_OFF")).thenReturn(Optional.of(compOff));
        when(types.existsById(any())).thenReturn(true);
        when(employees.existsById(EMP)).thenReturn(true);
        when(balances.findByEmployeeIdAndLeaveTypeIdAndYear(any(), any(), any())).thenReturn(Optional.empty());
        when(balances.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(currentUser.find()).thenReturn(Optional.empty());
    }

    private LeaveTransaction onlyLedgerEntry() {
        ArgumentCaptor<LeaveTransaction> captor = ArgumentCaptor.forClass(LeaveTransaction.class);
        verify(ledger, times(1)).save(captor.capture());
        return captor.getValue();
    }

    private LeaveBalance existing(String total, String used, String adjusted, int year) {
        LeaveBalance b = new LeaveBalance(1, EMP, CASUAL, new BigDecimal(total), new BigDecimal(used), new BigDecimal(adjusted), year);
        when(balances.findByEmployeeIdAndLeaveTypeIdAndYear(EMP, CASUAL, year)).thenReturn(Optional.of(b));
        return b;
    }

    // --- Creating balances ---

    @Test
    void createBalanceWritesAnAllocationEntry() {
        LeaveBalance b = service.createLeaveBalance(EMP, CASUAL, new BigDecimal("12.5"), YEAR);
        assertThat(b.getRemainingDays()).isEqualByComparingTo("12.5");
        LeaveTransaction t = onlyLedgerEntry();
        assertThat(t.getType()).isEqualTo(TransactionType.ALLOCATION);
        assertThat(t.getDays()).isEqualByComparingTo("12.5");
        assertThat(t.getYear()).isEqualTo(YEAR);
        assertThat(t.getCreatedByName()).isEqualTo("System"); // no logged-in user
    }

    @Test
    void compOffCannotBeAllotted() {
        assertThatThrownBy(() -> service.createLeaveBalance(EMP, COMP_OFF, BigDecimal.ONE, YEAR))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("credited");
    }

    @Test
    void allotmentMustBePositiveWholeOrHalfDays() {
        assertThatThrownBy(() -> service.createLeaveBalance(EMP, CASUAL, BigDecimal.ZERO, YEAR))
                .hasMessageContaining("greater than 0");
        assertThatThrownBy(() -> service.createLeaveBalance(EMP, CASUAL, new BigDecimal("12.25"), YEAR))
                .hasMessageContaining("whole or half days");
    }

    @Test
    void yearMustBeACalendarYear() {
        assertThatThrownBy(() -> service.createLeaveBalance(EMP, CASUAL, BigDecimal.TEN, 0))
                .hasMessageContaining("valid year");
        assertThatThrownBy(() -> service.createLeaveBalance(EMP, CASUAL, BigDecimal.TEN, 1999))
                .hasMessageContaining("valid year");
    }

    @Test
    void duplicateBalanceIsRefused() {
        existing("12", "0", "0", YEAR);
        assertThatThrownBy(() -> service.createLeaveBalance(EMP, CASUAL, BigDecimal.TEN, YEAR))
                .isInstanceOf(IllegalStateException.class);
        verify(ledger, never()).save(any());
    }

    @Test
    void unknownEmployeeIsRefused() {
        assertThatThrownBy(() -> service.createLeaveBalance(999, CASUAL, BigDecimal.TEN, YEAR))
                .isInstanceOf(NoSuchElementException.class);
    }

    // --- Leave taken and given back ---

    @Test
    void deductionWithoutABalanceGoesNegative() {
        LeaveBalance b = service.deductLeave(EMP, CASUAL, new BigDecimal("2"), YEAR, 55);
        assertThat(b.getRemainingDays()).isEqualByComparingTo("-2");
        LeaveTransaction t = onlyLedgerEntry();
        assertThat(t.getType()).isEqualTo(TransactionType.LEAVE_DEDUCTED);
        assertThat(t.getDays()).isEqualByComparingTo("-2");
        assertThat(t.getReferenceId()).isEqualTo(55);
    }

    @Test
    void restoringGivesTheDaysBack() {
        LeaveBalance b = existing("12", "3", "0", YEAR);
        service.restoreLeave(EMP, CASUAL, new BigDecimal("1.5"), YEAR, 55);
        assertThat(b.getUsedDays()).isEqualByComparingTo("1.5");
        assertThat(onlyLedgerEntry().getType()).isEqualTo(TransactionType.LEAVE_RESTORED);
    }

    @Test
    void compOffLeaveUsesTheBalanceWithoutAYear() {
        assertThat(service.balanceYear(COMP_OFF, YEAR)).isZero();
        assertThat(service.balanceYear(CASUAL, YEAR)).isEqualTo(YEAR);
        LeaveBalance b = service.deductLeave(EMP, COMP_OFF, BigDecimal.ONE, YEAR, 1);
        assertThat(b.getYear()).isEqualTo(LeaveBalance.NO_YEAR);
    }

    @Test
    void compOffCreditGoesToAdjustedDays() {
        LeaveBalance b = service.creditCompOff(EMP, new BigDecimal("2"), 9, "Worked on 2026-10-03");
        assertThat(b.getAdjustedDays()).isEqualByComparingTo("2");
        assertThat(b.getYear()).isZero();
        LeaveTransaction t = onlyLedgerEntry();
        assertThat(t.getType()).isEqualTo(TransactionType.COMP_OFF_CREDIT);
        assertThat(t.getReason()).isEqualTo("Worked on 2026-10-03");
    }

    // --- HR adjustments ---

    @Test
    void adjustmentNeedsDaysAndAReason() {
        assertThatThrownBy(() -> service.adjustBalance(EMP, CASUAL, YEAR, BigDecimal.ZERO, "x"))
                .hasMessageContaining("cannot be 0");
        assertThatThrownBy(() -> service.adjustBalance(EMP, CASUAL, YEAR, new BigDecimal("0.3"), "x"))
                .hasMessageContaining("whole or half days");
        assertThatThrownBy(() -> service.adjustBalance(EMP, CASUAL, YEAR, BigDecimal.ONE, "  "))
                .hasMessageContaining("reason");
    }

    @Test
    void negativeAdjustmentRemovesDaysAndIsLoggedWithTheReason() {
        LeaveBalance b = existing("12", "0", "0", YEAR);
        service.adjustBalance(EMP, CASUAL, YEAR, new BigDecimal("-2.5"), "  Unpaid absence ");
        assertThat(b.getRemainingDays()).isEqualByComparingTo("9.5");
        LeaveTransaction t = onlyLedgerEntry();
        assertThat(t.getType()).isEqualTo(TransactionType.ADJUSTMENT);
        assertThat(t.getReason()).isEqualTo("Unpaid absence");
    }

    @Test
    void compOffAdjustmentIgnoresTheYear() {
        LeaveBalance b = service.adjustBalance(EMP, COMP_OFF, 0, BigDecimal.ONE, "Correction");
        assertThat(b.getYear()).isZero();
    }

    // --- Yearly reset and lookups ---

    @Test
    void yearlyResetClearsUsedDaysAndSkipsUntouchedBalances() {
        LeaveBalance used = new LeaveBalance(1, EMP, CASUAL, BigDecimal.TEN, new BigDecimal("4"), BigDecimal.ZERO, YEAR);
        LeaveBalance unused = new LeaveBalance(2, EMP, 11, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, YEAR);
        when(balances.findByEmployeeIdAndYear(EMP, YEAR)).thenReturn(List.of(used, unused));
        service.resetYearlyLeaveBalance(EMP, YEAR);
        assertThat(used.getUsedDays()).isEqualByComparingTo("0");
        LeaveTransaction t = onlyLedgerEntry();
        assertThat(t.getType()).isEqualTo(TransactionType.RESET);
        assertThat(t.getDays()).isEqualByComparingTo("4");
    }

    @Test
    void missingBalanceCountsAsZero() {
        assertThat(service.getRemainingDays(EMP, CASUAL, YEAR)).isEqualByComparingTo("0");
        assertThat(service.hasSufficientLeaveBalance(EMP, CASUAL, BigDecimal.ONE, YEAR)).isFalse();
        existing("2", "0", "0", YEAR);
        assertThat(service.hasSufficientLeaveBalance(EMP, CASUAL, new BigDecimal("2"), YEAR)).isTrue();
    }
}
