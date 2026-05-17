package com.medcare.clinic_backend.config;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:Admin@123456}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (accountRepository.findByUsername(adminUsername).isEmpty()) {
            Account admin = new Account();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ROLE_ADMIN");
            accountRepository.save(admin);
            logger.info("Da tao tai khoan admin mac dinh: username={}", adminUsername);
        }
    }
}
