package com.leavemanagement;

import com.leavemanagement.entity.*;
import com.leavemanagement.repository.*;
import com.leavemanagement.security.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4: HR dashboard reports and the Team Lead "My team" view
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/test-${random.uuid}.db?date_class=TEXT")
class ReportTests {

    @Autowired MockMvc mockMvc;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired LeaveTypeRepository leaveTypeRepository;
    @Autowired PasswordEncoder passwordEncoder;

    UserAccount hrAccount, leadAccount, memberAccount, soloAccount;
    Employee hr, lead, member, solo;
    LeaveType casual, sick;
    LocalDate start = LocalDate.now().plusDays(10);
    int year = start.getYear();
    LocalDate lastSaturday = LocalDate.now().minusDays(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));

    @BeforeEach
    void setUp() throws Exception {
        hr = employee("Hana", "hr@test.com", "HR", null);
        hrAccount = account(hr, Role.HR_ADMIN);
        lead = employee("Lee", "lead@test.com", "IT", null);
        leadAccount = account(lead, Role.TEAM_LEAD);
        member = employee("Mia", "member@test.com", "IT", lead.getEmployeeId());
        memberAccount = account(member, Role.EMPLOYEE);
        solo = employee("Sam", "solo@test.com", "Sales", null);
        soloAccount = account(solo, Role.EMPLOYEE);

        casual = leaveTypeRepository.save(new LeaveType(null, "Casual Leave", 12, null, null));
        sick = leaveTypeRepository.save(new LeaveType(null, "Sick Leave", 10, null, null));
        createBalance(member, casual, "12").andExpect(status().isCreated());
        createBalance(solo, casual, "12").andExpect(status().isCreated());
        createBalance(solo, sick, "1").andExpect(status().isCreated());
    }

    @Test
    void summaryCountsEmployeesPendingRequestsAndDays() throws Exception {
        submitLeave(memberAccount, casual, start, start.plusDays(1));             // waits for the lead
        int soloLeave = submitLeave(soloAccount, casual, start, start);          // no lead: waits for HR
        approve("leave-requests", soloLeave, hrAccount);
        submitCompOff(memberAccount);                                            // waits for the lead

        mockMvc.perform(get("/api/reports/summary?year=" + year).with(as(hrAccount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(year))
                .andExpect(jsonPath("$.activeEmployees").value(4))
                .andExpect(jsonPath("$.leavePendingTeamLead").value(1))
                .andExpect(jsonPath("$.leavePendingHr").value(0))
                .andExpect(jsonPath("$.compOffPendingTeamLead").value(1))
                .andExpect(jsonPath("$.employeesWithNegativeBalance").value(0))
                .andExpect(jsonPath("$.totalAllotted").value(25))
                .andExpect(jsonPath("$.totalUsed").value(1))
                .andExpect(jsonPath("$.totalRemaining").value(24));
    }

    @Test
    void summaryOfAYearWithoutBalancesIsZero() throws Exception {
        mockMvc.perform(get("/api/reports/summary?year=1999").with(as(hrAccount)))
                .andExpect(jsonPath("$.totalAllotted").value(0))
                .andExpect(jsonPath("$.totalRemaining").value(0));
    }

    @Test
    void balanceReportListsEachEmployeeAndLeaveType() throws Exception {
        mockMvc.perform(get("/api/reports/leave-balance?year=" + year).with(as(hrAccount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].employeeName").value("Mia Test"))
                .andExpect(jsonPath("$[0].leadName").value("Lee Test"))
                .andExpect(jsonPath("$[0].department").value("IT"))
                .andExpect(jsonPath("$[0].remainingDays").value(12))
                .andExpect(jsonPath("$[1].employeeName").value("Sam Test"))
                .andExpect(jsonPath("$[1].leadName").doesNotExist());
    }

    @Test
    void balanceReportFiltersByDepartmentAndLeaveType() throws Exception {
        mockMvc.perform(get("/api/reports/leave-balance?year=%d&department=Sales".formatted(year)).with(as(hrAccount)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].employeeName", contains("Sam Test", "Sam Test")));
        mockMvc.perform(get("/api/reports/leave-balance?year=%d&leaveTypeId=%d".formatted(year, sick.getLeaveTypeId()))
                        .with(as(hrAccount)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].leaveTypeName").value("Sick Leave"));
    }

    @Test
    void balanceReportSeparatesCompOffCreditsFromAdjustments() throws Exception {
        int id = submitCompOff(memberAccount);
        approve("comp-off", id, leadAccount);
        approve("comp-off", id, hrAccount);
        adjust(member, casual, "-2", "Unpaid absence");

        mockMvc.perform(get("/api/reports/leave-balance?year=" + year).with(as(hrAccount)))
                .andExpect(jsonPath("$[?(@.employeeName == 'Mia Test' && @.year == 0)].compOffCredited", contains(1)))
                .andExpect(jsonPath("$[?(@.employeeName == 'Mia Test' && @.year == 0)].remainingDays", contains(1)))
                .andExpect(jsonPath("$[?(@.employeeName == 'Mia Test' && @.leaveTypeName == 'Casual Leave')].adjustments",
                        contains(-2)))
                .andExpect(jsonPath("$[?(@.employeeName == 'Mia Test' && @.leaveTypeName == 'Casual Leave')].compOffCredited",
                        contains(0)))
                .andExpect(jsonPath("$[?(@.employeeName == 'Mia Test' && @.leaveTypeName == 'Casual Leave')].remainingDays",
                        contains(10)));
    }

    @Test
    void negativeBalancesShowWhenTheyWentBelowZero() throws Exception {
        int id = submitLeave(soloAccount, sick, start, start.plusDays(2));
        approve("leave-requests", id, hrAccount);

        mockMvc.perform(get("/api/reports/negative-balances?year=" + year).with(as(hrAccount)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeName").value("Sam Test"))
                .andExpect(jsonPath("$[0].leaveTypeName").value("Sick Leave"))
                .andExpect(jsonPath("$[0].remainingDays").value(-2))
                .andExpect(jsonPath("$[0].negativeSince").value(LocalDate.now().toString()));
        mockMvc.perform(get("/api/reports/summary?year=" + year).with(as(hrAccount)))
                .andExpect(jsonPath("$.employeesWithNegativeBalance").value(1));
    }

    @Test
    void departmentsAreListedForTheFilter() throws Exception {
        mockMvc.perform(get("/api/reports/departments").with(as(hrAccount)))
                .andExpect(jsonPath("$", contains("HR", "IT", "Sales")));
    }

    @Test
    void teamLeadSeesOnlyTheirTeam() throws Exception {
        submitLeave(memberAccount, casual, start, start);
        submitLeave(soloAccount, casual, start, start);

        mockMvc.perform(get("/api/reports/team?year=" + year).with(as(leadAccount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamSize").value(1))
                .andExpect(jsonPath("$.waitingForMe").value(1))
                .andExpect(jsonPath("$.waitingForHr").value(0))
                .andExpect(jsonPath("$.balances[*].employeeName", contains("Mia Test")));
    }

    @Test
    void reportsAreLimitedByRole() throws Exception {
        for (String url : new String[]{"/api/reports/summary", "/api/reports/leave-balance",
                "/api/reports/negative-balances", "/api/reports/departments"}) {
            mockMvc.perform(get(url).with(as(leadAccount))).andExpect(status().isForbidden());
            mockMvc.perform(get(url).with(as(memberAccount))).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/reports/team").with(as(memberAccount))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/team").with(as(hrAccount))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardPageIsForHrAndTeamLeads() throws Exception {
        mockMvc.perform(get("/dashboard").with(as(hrAccount))).andExpect(status().isOk());
        mockMvc.perform(get("/dashboard").with(as(leadAccount))).andExpect(status().isOk());
        mockMvc.perform(get("/dashboard").with(as(memberAccount))).andExpect(status().isForbidden());
    }

    @Test
    void yearZeroIsRejected() throws Exception {
        mockMvc.perform(get("/api/reports/summary?year=0").with(as(hrAccount))).andExpect(status().isBadRequest());
    }

    // --- Helpers ---

    private ResultActions createBalance(Employee e, LeaveType type, String days) throws Exception {
        return mockMvc.perform(post("/api/leave-balance?employeeId=%d&leaveTypeId=%d&totalDays=%s&year=%d"
                .formatted(e.getEmployeeId(), type.getLeaveTypeId(), days, year)).with(as(hrAccount)).with(csrf()));
    }

    private int submitLeave(UserAccount account, LeaveType type, LocalDate from, LocalDate to) throws Exception {
        String body = """
                {"leaveTypeId":%d,"startDate":"%s","endDate":"%s","reason":"Holiday"}
                """.formatted(type.getLeaveTypeId(), from, to);
        String json = mockMvc.perform(post("/api/leave-requests").with(as(account)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Integer.parseInt(json.replaceAll(".*\"leaveRequestId\":(\\d+).*", "$1"));
    }

    private int submitCompOff(UserAccount account) throws Exception {
        String body = """
                {"workedFrom":"%s","workedTo":"%s","reason":"Release weekend"}
                """.formatted(lastSaturday, lastSaturday);
        String json = mockMvc.perform(post("/api/comp-off").with(as(account)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Integer.parseInt(json.replaceAll(".*\"compOffRequestId\":(\\d+).*", "$1"));
    }

    private void approve(String api, int id, UserAccount account) throws Exception {
        mockMvc.perform(post("/api/" + api + "/" + id + "/approve").with(as(account)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    private void adjust(Employee e, LeaveType type, String days, String reason) throws Exception {
        String body = """
                {"employeeId":%d,"leaveTypeId":%d,"year":%d,"days":%s,"reason":"%s"}
                """.formatted(e.getEmployeeId(), type.getLeaveTypeId(), year, days, reason);
        mockMvc.perform(post("/api/leave-balance/adjust").with(as(hrAccount)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private RequestPostProcessor as(UserAccount account) {
        return user(new AppUserDetails(userAccountRepository.findById(account.getUserId()).orElseThrow()));
    }

    private Employee employee(String firstName, String email, String department, Integer leadId) {
        Employee e = new Employee();
        e.setFirstName(firstName);
        e.setLastName("Test");
        e.setEmail(email);
        e.setDepartment(department);
        e.setDesignation("Engineer");
        e.setDateOfJoining(LocalDate.of(2024, 1, 15));
        e.setSalary(50000.0);
        e.setStatus(EmployeeStatus.ACTIVE);
        e.setReporting_LeadId(leadId);
        return employeeRepository.save(e);
    }

    private UserAccount account(Employee e, Role role) {
        UserAccount a = new UserAccount();
        a.setUsername(e.getEmail());
        a.setPasswordHash(passwordEncoder.encode("Secret123!"));
        a.setRole(role);
        a.setEmployeeId(e.getEmployeeId());
        a.setEnabled(true);
        a.setMustChangePassword(false);
        return userAccountRepository.save(a);
    }
}
