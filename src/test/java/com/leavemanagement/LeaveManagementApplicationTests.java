package com.leavemanagement;

import com.leavemanagement.entity.Role;
import com.leavemanagement.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/test-${random.uuid}.db?date_class=TEXT")
class LeaveManagementApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void contextLoadsAndCreatesInitialAdmin() {
        assertThat(userAccountRepository.existsByRole(Role.HR_ADMIN)).isTrue();
    }

    @Test
    void apiDocsAreHrOnly() throws Exception {
        mockMvc.perform(get("/api-docs").with(user("emp").roles("EMPLOYEE"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api-docs").with(user("hr").roles("HR_ADMIN"))).andExpect(status().isOk());
    }
}
