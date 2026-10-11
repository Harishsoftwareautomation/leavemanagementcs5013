package com.leavemanagement.service;

import com.leavemanagement.entity.LeaveBalance;
import com.leavemanagement.entity.LeaveTransaction;
import com.leavemanagement.entity.LeaveType;
import com.leavemanagement.entity.TransactionType;
import com.leavemanagement.repository.EmployeeRepository;
import com.leavemanagement.repository.LeaveBalanceRepository;
import com.leavemanagement.repository.LeaveTransactionRepository;
import com.leavemanagement.repository.LeaveTypeRepository;
import com.leavemanagement.security.AppUserDetails;
import com.leavemanagement.security.CurrentUser;
import com.leavemanagement.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Leave balances and their ledger. Every balance change goes through this class and writes one
 * LeaveTransaction, so the ledger always adds up to the balance.
 *
 * Balances may go below zero (no lower limit). The comp-off balance does not belong to a year:
 * it is stored with year = 0 and is used whatever year the leave falls in, so credits never expire.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LeaveService {
    public static final String COMP_OFF_CODE = "COMP_OFF";
    private static final BigDecimal HALF = new BigDecimal("0.5");

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTransactionRepository leaveTransactionRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUser currentUser;

    /**
     * The built-in Compensatory Leave type
     */
    public LeaveType getCompOffLeaveType() {
        return leaveTypeRepository.findBySystemCode(COMP_OFF_CODE)
                .orElseThrow(() -> new IllegalStateException("The Compensatory Leave type is missing"));
    }

    public boolean isCompOff(Integer leaveTypeId) {
        return leaveTypeRepository.findById(leaveTypeId).map(t -> COMP_OFF_CODE.equals(t.getSystemCode())).orElse(false);
    }

    /**
     * The year a balance is kept under: the leave's year, or 0 for the comp-off balance
     */
    public int balanceYear(Integer leaveTypeId, int year) {
        return isCompOff(leaveTypeId) ? LeaveBalance.NO_YEAR : year;
    }

    /**
     * Get leave balance for an employee for a specific leave type (the comp-off balance whatever the year)
     */
    public Optional<LeaveBalance> getLeaveBalance(Integer employeeId, Integer leaveTypeId, Integer year) {
        return leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(
                employeeId, leaveTypeId, balanceYear(leaveTypeId, year));
    }

    /**
     * Remaining days, treating a missing balance as 0
     */
    public BigDecimal getRemainingDays(Integer employeeId, Integer leaveTypeId, Integer year) {
        return getLeaveBalance(employeeId, leaveTypeId, year).map(LeaveBalance::getRemainingDays).orElse(BigDecimal.ZERO);
    }

    /**
     * Check if employee has sufficient leave balance
     */
    public boolean hasSufficientLeaveBalance(Integer employeeId, Integer leaveTypeId, BigDecimal daysRequired, Integer year) {
        return getRemainingDays(employeeId, leaveTypeId, year).compareTo(daysRequired) >= 0;
    }

    /**
     * All balances shown for a year: that year's balances plus the comp-off balance
     */
    public List<LeaveBalance> getAllLeaveBalances(Integer employeeId, Integer year) {
        return leaveBalanceRepository.findByEmployeeIdAndYearInOrderByLeaveTypeId(
                employeeId, List.of(year, LeaveBalance.NO_YEAR));
    }

    /**
     * Ledger entries for a year (and the comp-off balance), oldest first
     */
    public List<LeaveTransaction> getTransactions(Integer employeeId, Integer year) {
        return leaveTransactionRepository.findByEmployeeIdAndYearInOrderByCreatedAtAscTransactionIdAsc(
                employeeId, List.of(year, LeaveBalance.NO_YEAR));
    }

    /**
     * Create the yearly balance for an employee (HR). Comp-off days are credited, not allotted.
     */
    public LeaveBalance createLeaveBalance(Integer employeeId, Integer leaveTypeId, BigDecimal totalDays, Integer year) {
        if (isCompOff(leaveTypeId)) {
            throw new IllegalArgumentException(
                    "Compensatory leave is credited through comp-off requests or balance adjustments, not allotted");
        }
        requireEmployeeAndType(employeeId, leaveTypeId);
        requireCalendarYear(year);
        requireHalfDays(totalDays, "Total days");
        if (totalDays.signum() <= 0) {
            throw new IllegalArgumentException("Total days must be greater than 0");
        }
        if (getLeaveBalance(employeeId, leaveTypeId, year).isPresent()) {
            throw new IllegalStateException("A balance for this employee, leave type and year already exists");
        }
        LeaveBalance balance = leaveBalanceRepository.save(
                new LeaveBalance(null, employeeId, leaveTypeId, totalDays, BigDecimal.ZERO, BigDecimal.ZERO, year));
        log(balance, totalDays, TransactionType.ALLOCATION, null, "Balance set up for " + year);
        return balance;
    }

    /**
     * Deduct approved leave. No lower limit: the balance may go negative; a missing balance is created at 0.
     */
    public LeaveBalance deductLeave(Integer employeeId, Integer leaveTypeId, BigDecimal days, Integer year, Integer leaveRequestId) {
        LeaveBalance balance = getOrCreate(employeeId, leaveTypeId, year);
        balance.setUsedDays(balance.getUsedDays().add(days));
        leaveBalanceRepository.save(balance);
        log(balance, days.negate(), TransactionType.LEAVE_DEDUCTED, leaveRequestId, "Leave approved");
        return balance;
    }

    /**
     * Give back the days of a cancelled approved leave
     */
    public LeaveBalance restoreLeave(Integer employeeId, Integer leaveTypeId, BigDecimal days, Integer year, Integer leaveRequestId) {
        LeaveBalance balance = getOrCreate(employeeId, leaveTypeId, year);
        balance.setUsedDays(balance.getUsedDays().subtract(days));
        leaveBalanceRepository.save(balance);
        log(balance, days, TransactionType.LEAVE_RESTORED, leaveRequestId, "Approved leave cancelled");
        return balance;
    }

    /**
     * Credit approved comp-off days to the employee's comp-off balance
     */
    public LeaveBalance creditCompOff(Integer employeeId, BigDecimal days, Integer compOffRequestId, String reason) {
        LeaveBalance balance = getOrCreate(employeeId, getCompOffLeaveType().getLeaveTypeId(), LeaveBalance.NO_YEAR);
        balance.setAdjustedDays(balance.getAdjustedDays().add(days));
        leaveBalanceRepository.save(balance);
        log(balance, days, TransactionType.COMP_OFF_CREDIT, compOffRequestId, reason);
        return balance;
    }

    /**
     * HR correction: add (positive) or remove (negative) days, with a reason. Creates the balance if missing.
     */
    public LeaveBalance adjustBalance(Integer employeeId, Integer leaveTypeId, Integer year, BigDecimal days, String reason) {
        requireEmployeeAndType(employeeId, leaveTypeId);
        if (!isCompOff(leaveTypeId)) {
            requireCalendarYear(year);
        }
        if (days == null || days.signum() == 0) {
            throw new IllegalArgumentException("Adjustment days cannot be 0");
        }
        requireHalfDays(days, "Adjustment days");
        if (ValidationUtil.isEmpty(reason)) {
            throw new IllegalArgumentException("Please give a reason for the adjustment");
        }
        LeaveBalance balance = getOrCreate(employeeId, leaveTypeId, year);
        balance.setAdjustedDays(balance.getAdjustedDays().add(days));
        leaveBalanceRepository.save(balance);
        log(balance, days, TransactionType.ADJUSTMENT, null, reason.trim());
        return balance;
    }

    /**
     * Reset used days to 0 for the year (the comp-off balance is not affected)
     */
    public void resetYearlyLeaveBalance(Integer employeeId, Integer year) {
        requireCalendarYear(year);
        for (LeaveBalance balance : leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)) {
            BigDecimal used = balance.getUsedDays();
            if (used.signum() == 0) {
                continue;
            }
            balance.setUsedDays(BigDecimal.ZERO);
            leaveBalanceRepository.save(balance);
            log(balance, used, TransactionType.RESET, null, "Yearly reset of used days");
        }
    }

    private LeaveBalance getOrCreate(Integer employeeId, Integer leaveTypeId, Integer year) {
        int balanceYear = balanceYear(leaveTypeId, year);
        return leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveTypeId, balanceYear)
                .orElseGet(() -> leaveBalanceRepository.save(new LeaveBalance(
                        null, employeeId, leaveTypeId, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, balanceYear)));
    }

    private void log(LeaveBalance balance, BigDecimal days, TransactionType type, Integer referenceId, String reason) {
        LeaveTransaction t = new LeaveTransaction();
        t.setEmployeeId(balance.getEmployeeId());
        t.setLeaveTypeId(balance.getLeaveTypeId());
        t.setYear(balance.getYear());
        t.setDays(days);
        t.setType(type);
        t.setReferenceId(referenceId);
        t.setReason(reason);
        Optional<AppUserDetails> actor = currentUser.find();
        t.setCreatedByUserId(actor.map(AppUserDetails::getUserId).orElse(null));
        t.setCreatedByName(actor.map(this::displayName).orElse("System"));
        t.setCreatedAt(LocalDateTime.now());
        leaveTransactionRepository.save(t);
    }

    private String displayName(AppUserDetails actor) {
        if (actor.getEmployeeId() == null) {
            return actor.getUsername();
        }
        return employeeRepository.findById(actor.getEmployeeId())
                .map(e -> e.getFullName())
                .orElse(actor.getUsername());
    }

    private void requireEmployeeAndType(Integer employeeId, Integer leaveTypeId) {
        if (employeeId == null || !employeeRepository.existsById(employeeId)) {
            throw new NoSuchElementException("Employee not found");
        }
        if (leaveTypeId == null || !leaveTypeRepository.existsById(leaveTypeId)) {
            throw new NoSuchElementException("Leave type not found");
        }
    }

    /**
     * Yearly balances need a real year; 0 is reserved for the comp-off balance
     */
    private static void requireCalendarYear(Integer year) {
        if (year == null || year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Please give a valid year (2000-2100)");
        }
    }

    /**
     * Day values must be whole or half days (e.g. 3, 2.5)
     */
    private static void requireHalfDays(BigDecimal days, String field) {
        if (days == null || days.remainder(HALF).signum() != 0) {
            throw new IllegalArgumentException(field + " must be in whole or half days");
        }
    }
}
