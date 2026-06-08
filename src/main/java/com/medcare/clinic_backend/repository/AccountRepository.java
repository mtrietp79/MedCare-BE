package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    // Hàm cực kỳ quan trọng để Spring Security tìm user khi đăng nhập
    Optional<Account> findByUsername(String username);

    Optional<Account> findByResetToken(String resetToken);

    boolean existsByUsername(String username);
}