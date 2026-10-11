package com.leavemanagement.config;

import com.leavemanagement.entity.Role;
import com.leavemanagement.entity.UserAccount;
import com.leavemanagement.repository.UserAccountRepository;
import com.leavemanagement.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the initial HR admin account on first start, so someone can log in and create employees
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userAccountRepository.existsByRole(Role.HR_ADMIN)) {
            return;
        }
        boolean generated = adminPassword == null || adminPassword.isBlank();
        String password = generated ? PasswordGenerator.generate() : adminPassword;

        UserAccount admin = new UserAccount();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.HR_ADMIN);
        admin.setEnabled(true);
        admin.setMustChangePassword(true);
        userAccountRepository.save(admin);

        if (generated) {
            log.warn("Created initial HR admin account '{}' with temporary password: {}  "
                    + "(set LMS_ADMIN_PASSWORD to choose it; you must change it at first login)", adminUsername, password);
        } else {
            log.info("Created initial HR admin account '{}' (password from LMS_ADMIN_PASSWORD)", adminUsername);
        }
    }
}
