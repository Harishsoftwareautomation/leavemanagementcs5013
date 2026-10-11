package com.leavemanagement.service;

import com.leavemanagement.dto.ApprovalView;
import com.leavemanagement.dto.LeaveRequestView;
import com.leavemanagement.entity.Employee;
import com.leavemanagement.entity.LeaveApproval;
import com.leavemanagement.entity.LeaveRequest;
import com.leavemanagement.entity.LeaveType;
import com.leavemanagement.entity.RequestType;
import com.leavemanagement.repository.EmployeeRepository;
import com.leavemanagement.repository.LeaveApprovalRepository;
import com.leavemanagement.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adds employee, reporting lead and leave type names, approval history and the balance
 * after approval (pending requests only) to leave requests, loading related data in bulk
 */
@Component
@RequiredArgsConstructor
public class LeaveRequestViewMapper {
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveApprovalRepository leaveApprovalRepository;
    private final LeaveService leaveService;

    public LeaveRequestView toView(LeaveRequest request) {
        return toViews(List.of(request)).get(0);
    }

    public List<LeaveRequestView> toViews(List<LeaveRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        Map<Integer, Employee> employees = employeeRepository.findAllById(
                        requests.stream().map(LeaveRequest::getEmployeeId).distinct().toList())
                .stream().collect(Collectors.toMap(Employee::getEmployeeId, Function.identity()));
        Map<Integer, String> leadNames = employeeRepository.findAllById(
                        employees.values().stream().map(Employee::getReporting_LeadId)
                                .filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Employee::getEmployeeId, Employee::getFullName));
        Map<Integer, String> types = leaveTypeRepository.findAll().stream()
                .collect(Collectors.toMap(LeaveType::getLeaveTypeId, LeaveType::getLeaveTypeName));
        Map<Integer, List<ApprovalView>> approvals = leaveApprovalRepository
                .findByRequestTypeAndRequestIdInOrderByActedAtAscApprovalIdAsc(RequestType.LEAVE,
                        requests.stream().map(LeaveRequest::getLeaveRequestId).toList())
                .stream().collect(Collectors.groupingBy(LeaveApproval::getRequestId,
                        Collectors.mapping(ApprovalView::of, Collectors.toList())));
        Map<String, BigDecimal> remaining = remainingBalances(requests);

        return requests.stream().map(r -> {
            Employee employee = employees.get(r.getEmployeeId());
            Integer leadId = employee == null ? null : employee.getReporting_LeadId();
            BigDecimal remainingDays = remaining.get(balanceKey(r.getEmployeeId(), r.getLeaveTypeId(), r.getStartDate().getYear()));
            return new LeaveRequestView(
                    r.getLeaveRequestId(),
                    r.getEmployeeId(),
                    employee == null ? null : employee.getFullName(),
                    leadId == null ? null : leadNames.get(leadId),
                    r.getLeaveTypeId(),
                    types.get(r.getLeaveTypeId()),
                    r.getStartDate(),
                    r.getEndDate(),
                    r.getHalfDay(),
                    r.getNumberOfDays(),
                    r.getReason(),
                    r.getStatus(),
                    r.getRequestDate(),
                    remainingDays == null ? null : remainingDays.subtract(r.getNumberOfDays()),
                    approvals.getOrDefault(r.getLeaveRequestId(), List.of()));
        }).toList();
    }

    /**
     * Remaining days (a missing balance counts as 0) for each pending request's employee, leave type and year
     */
    private Map<String, BigDecimal> remainingBalances(List<LeaveRequest> requests) {
        Map<String, BigDecimal> remaining = new HashMap<>();
        for (LeaveRequest r : requests) {
            if (r.getStatus().isPending()) {
                remaining.computeIfAbsent(balanceKey(r.getEmployeeId(), r.getLeaveTypeId(), r.getStartDate().getYear()),
                        k -> leaveService.getRemainingDays(r.getEmployeeId(), r.getLeaveTypeId(), r.getStartDate().getYear()));
            }
        }
        return remaining;
    }

    private static String balanceKey(Integer employeeId, Integer leaveTypeId, Integer year) {
        return employeeId + ":" + leaveTypeId + ":" + year;
    }
}
