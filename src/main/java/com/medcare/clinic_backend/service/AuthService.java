package com.medcare.clinic_backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.medcare.clinic_backend.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.security.SecureRandom;
import java.util.Set;
import java.util.UUID;
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

    @Value("${auth.google.client-id:}")
    private String googleClientId;

    @Transactional
    public String register(Account account) {
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
        if (accountRepository.findByUsername(normalizedUsername).isPresent()) {
            return "Loi: Tai khoan da ton tai tren he thong!";
        }

        createAccount(normalizedUsername, account.getPassword(), normalizedRole, null, false);
        return "Dang ky thanh cong!";
    }

    public String registerDoctorAccount(String username, String password) {
        Account doctorAccount = new Account(username, password, "ROLE_DOCTOR");
        return register(doctorAccount);
    }

    public String getRoleByUsername(String username) {
        return accountRepository.findByUsername(username)
                .map(Account::getRole)
                .map(this::normalizeRole)
                .orElse("ROLE_PATIENT");
    }

    @Transactional
    public Map<String, String> processForgotPassword(String username) {
        String normalizedUsername = normalizeIdentifier(username);
        if (normalizedUsername == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chi ho tro khoi phuc bang Gmail hoac so dien thoai.");
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
        String normalizedUsername = normalizeIdentifier(username);
        if (normalizedUsername == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chi ho tro khoi phuc bang Gmail hoac so dien thoai.");
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

    public String loginWithGoogle(String idTokenString) throws Exception {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "He thong chua cau hinh Google Client ID.");
        }

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new Exception("Token Google khong hop le!");
        }
        return findOrCreateSocialAccount(idToken.getPayload().getEmail());
    }

    public String loginWithFacebook(String accessToken) throws Exception {
        String fbUrl = "https://graph.facebook.com/me?fields=id,name,email&access_token=" + accessToken;
        RestTemplate restTemplate = new RestTemplate();
        try {
            Map<String, Object> userData = restTemplate.getForEntity(fbUrl, Map.class).getBody();
            if (userData == null || !userData.containsKey("email")) {
                throw new Exception("Loi lay email Facebook");
            }
            return findOrCreateSocialAccount((String) userData.get("email"));
        } catch (Exception ex) {
            throw new Exception("Xac thuc Facebook that bai!");
        }
    }

    private String findOrCreateSocialAccount(String email) {
        String normalizedEmail = normalizeText(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Tai khoan social khong co email hop le.");
        }
        normalizedEmail = normalizedEmail.toLowerCase();

        String finalEmail = normalizedEmail;
        return accountRepository.findByUsername(normalizedEmail)
                .map(Account::getUsername)
                .orElseGet(() -> {
                    createAccount(finalEmail, generateRandomSocialPassword(), "ROLE_PATIENT", null, false);
                    return finalEmail;
                });
    }

    private void createAccount(
            String username,
            String password,
            String normalizedRole,
            String fullName,
            boolean passwordAlreadyEncoded
    ) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordAlreadyEncoded ? password : passwordEncoder.encode(password));
        account.setRole(normalizedRole);
        Account savedAccount = accountRepository.save(account);
        if ("ROLE_PATIENT".equals(normalizedRole)) {
            patientService.createInitialProfileForAccount(savedAccount, fullName);
        }
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

    private boolean isGmail(String value) {
        return value != null && GMAIL_PATTERN.matcher(value).matches();
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

    private String generateRandomSocialPassword() {
        return "SOCIAL-" + UUID.randomUUID();
    }
}
