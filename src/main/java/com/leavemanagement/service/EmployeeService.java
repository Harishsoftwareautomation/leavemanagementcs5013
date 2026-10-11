package com.leavemanagement.service;

import com.leavemanagement.dto.CreateEmployeeRequest;
import com.leavemanagement.dto.CreatedEmployeeResponse;
import com.leavemanagement.dto.EmployeeView;
import com.leavemanagement.dto.UpdateEmployeeRequest;
import com.leavemanagement.entity.Employee;
import com.leavemanagement.entity.EmployeeStatus;
import com.leavemanagement.entity.Role;
import com.leavemanagement.entity.UserAccount;
import com.leavemanagement.repository.EmployeeRepository;
import com.leavemanagement.repository.UserAccountRepository;
import com.leavemanagement.util.PasswordGenerator;
import com.leavemanagement.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service class for managing employee records and their login accounts
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create an employee and their login account (username = email) with a temporary password
     */
    public CreatedEmployeeResponse createEmployee(CreateEmployeeRequest req) {
        String email = req.email().trim();
        if (employeeRepository.existsByEmailIgnoreCase(email) || userAccountRepository.existsByUsernameIgnoreCase(email)) {
            throw new IllegalStateException("An employee or account with this email already exists");
        }
        if (req.reporting_LeadId() != null) {
            validateReportingLead(req.reporting_LeadId(), null);
        }

        Employee employee = new Employee();
        employee.setFirstName(req.firstName().trim());
        employee.setLastName(req.lastName().trim());
        employee.setEmail(email);
        employee.setPhone(blankToNull(req.phone()));
        employee.setDepartment(req.department().trim());
        employee.setDesignation(req.designation().trim());
        employee.setDateOfJoining(req.dateOfJoining());
        employee.setSalary(req.salary());
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setReporting_LeadId(req.reporting_LeadId());
        employee = employeeRepository.save(employee);

        String temporaryPassword = PasswordGenerator.generate();
        UserAccount account = new UserAccount();
        account.setUsername(email);
        account.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        account.setRole(req.role());
        account.setEmployeeId(employee.getEmployeeId());
        account.setEnabled(true);
        account.setMustChangePassword(true);
        userAccountRepository.save(account);

        return new CreatedEmployeeResponse(toView(employee, true), email, temporaryPassword);
    }

    /**
     * HR update of an employee. Changing the email also changes the login username;
     * a status other than ACTIVE disables the login account.
     */
    public Employee updateEmployee(Integer employeeId, UpdateEmployeeRequest req) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));
        UserAccount account = userAccountRepository.findByEmployeeId(employeeId).orElse(null);

        if (req.firstName() != null) employee.setFirstName(requireText(req.firstName(), "First name"));
        if (req.lastName() != null) employee.setLastName(requireText(req.lastName(), "Last name"));
        if (req.department() != null) employee.setDepartment(requireText(req.department(), "Department"));
        if (req.designation() != null) employee.setDesignation(requireText(req.designation(), "Designation"));
        if (req.phone() != null) employee.setPhone(blankToNull(req.phone()));
        if (req.salary() != null) employee.setSalary(req.salary());

        if (req.email() != null && !req.email().trim().equalsIgnoreCase(employee.getEmail())) {
            String email = req.email().trim();
            if (employeeRepository.existsByEmailIgnoreCase(email) || userAccountRepository.existsByUsernameIgnoreCase(email)) {
                throw new IllegalStateException("An employee or account with this email already exists");
            }
            employee.setEmail(email);
            if (account != null) account.setUsername(email);
        }

        if (Boolean.TRUE.equals(req.clearReportingLead())) {
            employee.setReporting_LeadId(null);
        } else if (req.reporting_LeadId() != null) {
            validateReportingLead(req.reporting_LeadId(), employeeId);
            employee.setReporting_LeadId(req.reporting_LeadId());
        }

        if (req.status() != null) {
            employee.setStatus(req.status());
            if (account != null) account.setEnabled(req.status() == EmployeeStatus.ACTIVE);
        }

        if (req.role() != null && account != null && req.role() != account.getRole()) {
            if (account.getRole() == Role.TEAM_LEAD && req.role() == Role.EMPLOYEE
                    && !employeeRepository.findTeamMemberIds(employeeId).isEmpty()) {
                throw new IllegalStateException("Reassign this lead's team members before changing their role");
            }
            account.setRole(req.role());
        }

        if (account != null) userAccountRepository.save(account);
        return employeeRepository.save(employee);
    }

    /**
     * Issue a new temporary password; the employee must change it at next login
     */
    public String resetPassword(Integer employeeId) {
        UserAccount account = userAccountRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new NoSuchElementException("This employee has no login account"));
        String temporaryPassword = PasswordGenerator.generate();
        account.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        account.setMustChangePassword(true);
        account.revokeTokens(); // logs the user out everywhere
        userAccountRepository.save(account);
        return temporaryPassword;
    }

    /**
     * Update the logged-in employee's own contact details
     */
    public Employee updateOwnPhone(Integer employeeId, String phone) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));
        employee.setPhone(blankToNull(phone));
        return employeeRepository.save(employee);
    }

    /**
     * Get employee by ID
     */
    public Optional<Employee> getEmployeeById(Integer employeeId) {
        return employeeRepository.findById(employeeId);
    }

    /**
     * Get employee by email
     */
    public Optional<Employee> getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }

    /**
     * Deactivate employee (set status to inactive and disable login)
     */
    public void deactivateEmployee(Integer employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));
        employee.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.save(employee);
        userAccountRepository.findByEmployeeId(employeeId).ifPresent(account -> {
            account.setEnabled(false);
            userAccountRepository.save(account);
        });
    }

    /**
     * Get all active employees
     */
    public List<Employee> getAllActiveEmployees() {
        return employeeRepository.findByStatus(EmployeeStatus.ACTIVE);
    }

    /**
     * Get all employees regardless of status (HR view)
     */
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    /**
     * Get the employees who report to a Team Lead
     */
    public List<Employee> getTeamMembers(Integer leadId) {
        return employeeRepository.findTeamMembers(leadId);
    }

    /**
     * Get employees by department
     */
    public List<Employee> getEmployeesByDepartment(String department) {
        return employeeRepository.findByDepartmentAndStatus(department, EmployeeStatus.ACTIVE);
    }

    /**
     * Search employees by name
     */
    public List<Employee> searchEmployeesByName(String name) {
        return employeeRepository.searchByName(name);
    }

    /**
     * Get total active employees count
     */
    public long getTotalEmployeesCount() {
        return employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
    }

    public EmployeeView toView(Employee employee, boolean includeSalary) {
        return toViews(List.of(employee), includeSalary).get(0);
    }

    /**
     * Add role and reporting lead name to employees, loading accounts and leads in bulk
     */
    public List<EmployeeView> toViews(List<Employee> employees, boolean includeSalary) {
        List<Integer> ids = employees.stream().map(Employee::getEmployeeId).toList();
        Map<Integer, Role> roles = userAccountRepository.findByEmployeeIdIn(ids).stream()
                .collect(Collectors.toMap(UserAccount::getEmployeeId, UserAccount::getRole));
        List<Integer> leadIds = employees.stream().map(Employee::getReporting_LeadId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Integer, Employee> leads = employeeRepository.findAllById(leadIds).stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, Function.identity()));
        return employees.stream()
                .map(e -> {
                    Employee lead = e.getReporting_LeadId() == null ? null : leads.get(e.getReporting_LeadId());
                    return EmployeeView.of(e, roles.get(e.getEmployeeId()),
                            lead == null ? null : lead.getFullName(), includeSalary);
                })
                .toList();
    }

    /**
     * A reporting lead must be an active employee with the TEAM_LEAD or HR_ADMIN role, and not the employee themselves
     */
    private void validateReportingLead(Integer leadId, Integer employeeId) {
        if (leadId.equals(employeeId)) {
            throw new IllegalArgumentException("An employee cannot be their own reporting lead");
        }
        Employee lead = employeeRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Reporting lead not found"));
        if (lead.getStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalArgumentException("Reporting lead is not an active employee");
        }
        Role leadRole = userAccountRepository.findByEmployeeId(leadId).map(UserAccount::getRole).orElse(null);
        if (leadRole != Role.TEAM_LEAD && leadRole != Role.HR_ADMIN) {
            throw new IllegalArgumentException("Reporting lead must have the Team Lead or HR role");
        }
    }

    private static String requireText(String value, String field) {
        if (ValidationUtil.isEmpty(value)) {
            throw new IllegalArgumentException(field + " cannot be empty");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return ValidationUtil.isEmpty(value) ? null : value.trim();
    }
}
