package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.medcare.clinic_backend.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.Map;
import java.security.SecureRandom;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_PATIENT", "ROLE_DOCTOR", "ROLE_ADMIN");
    private static final Pattern GMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)\\d{9,10}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PatientService patientService;

    @Autowired
    private OtpDeliveryService otpDeliveryService;

    @Transactional
    public String register(Account account) {
        return register(account, null, null, null);
    }

    @Transactional
    public String register(Account account, String fullName, String phone, String email) {
        if (account == null) {
            return "Loi: Du lieu tai khoan khong hop le!";
        }
        if (account.getUsername() == null || account.getUsername().isBlank()) {
            return "Loi: Username khong duoc de trong!";
        }
        if (account.getPassword() == null || account.getPassword().isBlank()) {
            return "Loi: Mat khau khong duoc de trong!";
        }

        String normalizedRole = normalizeRole(account.getRole());
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            return "Loi: Role khong hop le!";
        }
        String normalizedUsername = "ROLE_PATIENT".equals(normalizedRole)
                ? normalizeIdentifier(account.getUsername())
                : normalizeText(account.getUsername());
        if (normalizedUsername == null) {
            return "Loi: Patient chi duoc dang ky bang Gmail hoac so dien thoai!";
        }

        String normalizedPhone = normalizePhone(phone);
        String normalizedEmail = normalizeEmail(email);
        if (isGmail(normalizedUsername) && normalizedEmail == null) {
            normalizedEmail = normalizedUsername.toLowerCase();
        }
        if (isPhone(normalizedUsername) && normalizedPhone == null) {
            normalizedPhone = normalizedUsername;
        }

        if (accountRepository.findByUsername(normalizedUsername).isPresent()) {
            return "Loi: Tai khoan da ton tai tren he thong!";
        }

        createAccount(
                normalizedUsername,
                account.getPassword(),
                normalizedRole,
                fullName,
                normalizedPhone,
                normalizedEmail,
                false
        );
        return "Dang ky thanh cong!";
    }

    public String registerDoctorAccount(String username, String password) {
        Account doctorAccount = new Account(username, password, "ROLE_DOCTOR");
        return register(doctorAccount);
    }

    public String normalizeLoginIdentifier(String rawIdentifier) {
        if (rawIdentifier == null) return null;
        String trimmed = rawIdentifier.trim();
        if (trimmed.isEmpty()) return null;
        if (isGmail(trimmed)) return trimmed.toLowerCase();
        return trimmed;
    }

    public String resolveLoginUsername(String rawIdentifier) {
        String normalized = normalizeLoginIdentifier(rawIdentifier);
        if (normalized == null) {
            return null;
        }
        if (accountRepository.findByUsername(normalized).isPresent()) {
            return normalized;
        }
        if (isEmail(normalized)) {
            Account linkedAccount = patientService.findLinkedAccountByEmail(normalized);
            if (linkedAccount != null) {
                return linkedAccount.getUsername();
            }
        }
        if (isPhone(normalized)) {
            Account linkedAccount = patientService.findLinkedAccountByPhone(normalized);
            if (linkedAccount != null) {
                return linkedAccount.getUsername();
            }
        }
        return normalized;
    }

    public String getRoleByUsername(String username) {
        return accountRepository.findByUsername(username)
                .map(Account::getRole)
                .map(this::normalizeRole)
                .orElse("ROLE_PATIENT");
    }

    public Integer getAccountIdByUsername(String username) {
        return accountRepository.findByUsername(username)
                .map(Account::getId)
                .orElse(null);
    }

    @Transactional
    public Map<String, String> processForgotPassword(String username) {
        String normalizedUsername = resolveAccountUsernameForRecovery(username);
        if (normalizedUsername == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chi ho tro khoi phuc bang email hoac so dien thoai.");
        }

        Account account = accountRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Tai khoan khong ton tai!"));

        String otp = generateOtp();
        account.setResetOtp(otp);
        account.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        accountRepository.save(account);

        return otpDeliveryService.sendPasswordResetOtp(normalizedUsername, otp, isGmail(normalizedUsername));
    }

    @Transactional
    public void resetPassword(String username, String otp, String newPassword) {
        String normalizedUsername = resolveAccountUsernameForRecovery(username);
        if (normalizedUsername == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chi ho tro khoi phuc bang email hoac so dien thoai.");
        }
        if (otp == null || otp.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ma OTP khong duoc de trong.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Mat khau moi khong duoc de trong.");
        }

        Account account = accountRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Tai khoan khong ton tai!"));

        if (account.getResetOtp() == null || !account.getResetOtp().equals(otp)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ma OTP khong hop le!");
        }
        if (account.getOtpExpiryTime() == null || account.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ma OTP da het han!");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        account.setResetOtp(null);
        account.setOtpExpiryTime(null);
        accountRepository.save(account);
    }

    private Account createAccount(
            String username,
            String password,
            String normalizedRole,
            String fullName,
            String phone,
            String email,
            boolean passwordAlreadyEncoded
    ) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordAlreadyEncoded ? password : passwordEncoder.encode(password));
        account.setRole(normalizedRole);
        Account savedAccount = accountRepository.save(account);
        if ("ROLE_PATIENT".equals(normalizedRole)) {
            patientService.createInitialProfileForAccount(savedAccount, fullName, phone, email);
        }
        return savedAccount;
    }

    private String resolveAccountUsernameForRecovery(String rawIdentifier) {
        String normalized = normalizeLoginIdentifier(rawIdentifier);
        if (normalized == null) {
            return null;
        }
        if (accountRepository.findByUsername(normalized).isPresent()) {
            return normalized;
        }
        if (isEmail(normalized)) {
            Account linkedAccount = patientService.findLinkedAccountByEmail(normalized);
            if (linkedAccount != null) {
                return linkedAccount.getUsername();
            }
        }
        if (isPhone(normalized)) {
            Account linkedAccount = patientService.findLinkedAccountByPhone(normalized);
            if (linkedAccount != null) {
                return linkedAccount.getUsername();
            }
            return normalized;
        }
        return null;
    }

    private String normalizeRole(String role) {
        String normalized = role == null || role.isBlank() ? "ROLE_PATIENT" : role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private String normalizeIdentifier(String username) {
        String normalized = normalizeText(username);
        if (normalized == null) {
            return null;
        }
        if (isGmail(normalized)) {
            return normalized.toLowerCase();
        }
        return isPhone(normalized) ? normalized : null;
    }

    private String normalizePhone(String phone) {
        String normalized = normalizeText(phone);
        if (normalized == null) {
            return null;
        }
        if (!isPhone(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "So dien thoai khong hop le.");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeText(email);
        if (normalized == null) {
            return null;
        }
        String lower = normalized.toLowerCase();
        if (!lower.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Email khong hop le.");
        }
        return lower;
    }

    private boolean isGmail(String value) {
        return value != null && GMAIL_PATTERN.matcher(value).matches();
    }

    private boolean isEmail(String value) {
        return value != null && value.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isPhone(String value) {
        return value != null && PHONE_PATTERN.matcher(value).matches();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
