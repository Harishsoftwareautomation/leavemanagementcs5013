package com.leavemanagement.repository;

import com.leavemanagement.entity.Role;
import com.leavemanagement.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Integer> {
    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    Optional<UserAccount> findByEmployeeId(Integer employeeId);

    List<UserAccount> findByEmployeeIdIn(List<Integer> employeeIds);

    boolean existsByRole(Role role);

    boolean existsByUsernameIgnoreCase(String username);
}
