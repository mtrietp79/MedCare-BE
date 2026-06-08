package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AccountRepository;
import com.medcare.clinic_backend.util.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

    static final String GENERIC_OTP_MESSAGE =
            "Nếu email hợp lệ và có thể khôi phục, mã OTP sẽ được gửi đến email của bạn.";
    static final String ADMIN_REQUIRED_MESSAGE =
            "Tài khoản này không hỗ trợ tự khôi phục mật khẩu. Vui lòng liên hệ quản trị viên để được reset mật khẩu.";

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 10;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_OTP_FAILED_ATTEMPTS = 5;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$!";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PatientService patientService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private Environment environment;

    @Transactional
    public Map<String, String> requestForgotPasswordOtp(String rawEmail) {
        String normalizedEmail = normalizeEmail(rawEmail);
        Optional<Account> accountOptional = findRecoverablePatientAccount(normalizedEmail);

        if (accountOptional.isEmpty()) {
            return Map.of("message", GENERIC_OTP_MESSAGE);
        }

        Account account = accountOptional.get();
        if (!canSelfRecoverByOtp(account)) {
            return Map.of("message", ADMIN_REQUIRED_MESSAGE);
        }

        enforceOtpResendCooldown(account);

        String otp = generateOtp();
        account.setResetOtp(otp);
        account.setOtpExpiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        account.setOtpLastSentAt(LocalDateTime.now());
        account.setOtpFailedAttempts(0);
        account.setResetToken(null);
        account.setResetTokenExpiryTime(null);
        accountRepository.save(account);

        sendOtpEmail(normalizedEmail, otp);
        logDevOtp(normalizedEmail, otp);

        return Map.of("message", GENERIC_OTP_MESSAGE);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public Map<String, String> verifyForgotPasswordOtp(String rawEmail, String otp) {
        String normalizedEmail = normalizeEmail(rawEmail);
        Account account = findRecoverablePatientAccount(normalizedEmail)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ."));

        if (!canSelfRecoverByOtp(account)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ADMIN_REQUIRED_MESSAGE);
        }

        validateOtpAttempt(account, otp);

        String resetToken = UUID.randomUUID().toString().replace("-", "");
        account.setResetToken(resetToken);
        account.setResetTokenExpiryTime(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));
        account.setResetOtp(null);
        account.setOtpExpiryTime(null);
        account.setOtpFailedAttempts(0);
        accountRepository.save(account);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", "Xác nhận OTP thành công");
        response.put("resetToken", resetToken);
        return response;
    }

    @Transactional
    public Map<String, String> resetPasswordWithToken(String resetToken, String newPassword, String confirmPassword) {
        if (resetToken == null || resetToken.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Reset token không hợp lệ.");
        }

        Account account = accountRepository.findByResetToken(resetToken.trim())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Reset token không hợp lệ hoặc đã hết hạn."));

        if (account.getResetTokenExpiryTime() == null
                || account.getResetTokenExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Reset token không hợp lệ hoặc đã hết hạn.");
        }

        PasswordValidator.validatePasswordPair(newPassword, confirmPassword);
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setMustChangePassword(false);
        account.clearPasswordRecoveryState();
        accountRepository.save(account);

        return Map.of("message", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.");
    }

    @Transactional
    public Map<String, String> changePassword(String username, String oldPassword, String newPassword, String confirmPassword) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại."));

        if (oldPassword == null || oldPassword.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không được để trống.");
        }
        if (!passwordEncoder.matches(oldPassword, account.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không chính xác.");
        }

        PasswordValidator.validatePasswordPair(newPassword, confirmPassword);
        if (passwordEncoder.matches(newPassword, account.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng mật khẩu cũ.");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        account.setMustChangePassword(false);
        account.clearPasswordRecoveryState();
        accountRepository.save(account);

        return Map.of("message", "Đổi mật khẩu thành công");
    }

    @Transactional
    public Map<String, Object> adminResetPassword(Integer accountId, String temporaryPassword) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại."));

        String tempPassword = (temporaryPassword == null || temporaryPassword.isBlank())
                ? generateTemporaryPassword()
                : temporaryPassword.trim();

        PasswordValidator.validateNewPassword(tempPassword);

        account.setPassword(passwordEncoder.encode(tempPassword));
        account.setMustChangePassword(true);
        account.clearPasswordRecoveryState();
        accountRepository.save(account);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Reset mật khẩu thành công");
        response.put("temporaryPassword", tempPassword);
        response.put("mustChangePassword", true);
        return response;
    }

    public boolean mustChangePassword(String username) {
        return accountRepository.findByUsername(username)
                .map(account -> Boolean.TRUE.equals(account.getMustChangePassword()))
                .orElse(false);
    }

    private Optional<Account> findRecoverablePatientAccount(String normalizedEmail) {
        if (normalizedEmail == null) {
            return Optional.empty();
        }

        Optional<Account> direct = accountRepository.findByUsername(normalizedEmail);
        if (direct.isPresent()) {
            return direct;
        }

        Account linked = patientService.findLinkedAccountByEmail(normalizedEmail);
        return Optional.ofNullable(linked);
    }

    private boolean canSelfRecoverByOtp(Account account) {
        if (account == null) {
            return false;
        }
        String role = normalizeRole(account.getRole());
        if (!"ROLE_PATIENT".equals(role)) {
            return false;
        }
        return !Boolean.TRUE.equals(account.getIsTestAccount());
    }

    private void enforceOtpResendCooldown(Account account) {
        if (account.getOtpLastSentAt() == null) {
            return;
        }
        LocalDateTime nextAllowed = account.getOtpLastSentAt().plusSeconds(OTP_RESEND_COOLDOWN_SECONDS);
        if (LocalDateTime.now().isBefore(nextAllowed)) {
            throw new BusinessException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Vui lòng đợi " + OTP_RESEND_COOLDOWN_SECONDS + " giây trước khi yêu cầu OTP mới."
            );
        }
    }

    private void validateOtpAttempt(Account account, String otp) {
        if (account.getOtpFailedAttempts() != null && account.getOtpFailedAttempts() >= MAX_OTP_FAILED_ATTEMPTS) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Bạn đã nhập sai OTP quá nhiều lần. Vui lòng yêu cầu mã OTP mới."
            );
        }

        if (otp == null || otp.isBlank()) {
            incrementOtpFailedAttempts(account);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ.");
        }

        if (account.getResetOtp() == null || !account.getResetOtp().equals(otp.trim())) {
            incrementOtpFailedAttempts(account);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ.");
        }

        if (account.getOtpExpiryTime() == null || account.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            incrementOtpFailedAttempts(account);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn.");
        }
    }

    private void incrementOtpFailedAttempts(Account account) {
        int attempts = account.getOtpFailedAttempts() == null ? 0 : account.getOtpFailedAttempts();
        account.setOtpFailedAttempts(attempts + 1);
        accountRepository.save(account);
    }

    private void sendOtpEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã OTP khôi phục mật khẩu - MedCare");
        message.setText(
                "Mã OTP khôi phục mật khẩu của bạn là: " + otp
                        + ". Mã có hiệu lực trong " + OTP_EXPIRY_MINUTES + " phút."
        );
        mailSender.send(message);
    }

    private void logDevOtp(String email, String otp) {
        if (isDevProfile()) {
            log.info("[DEV ONLY] Reset OTP for {}: {}", email, otp);
        }
    }

    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Email không hợp lệ");
        }
        String normalized = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Email không hợp lệ");
        }
        return normalized;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_PATIENT";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder(12);
        builder.append('T').append('e').append('m').append('p');
        builder.append('@').append((char) ('0' + SECURE_RANDOM.nextInt(10)));
        for (int i = builder.length(); i < 12; i++) {
            builder.append(TEMP_PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return builder.toString();
    }
}
