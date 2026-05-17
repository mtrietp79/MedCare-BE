package com.medcare.clinic_backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.entity.SocialIdentity;
import com.medcare.clinic_backend.repository.AccountRepository;
import com.medcare.clinic_backend.repository.SocialIdentityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
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
    private SocialIdentityRepository socialIdentityRepository;

    @Autowired
    private OtpDeliveryService otpDeliveryService;

    @Value("${auth.google.client-id:}")
    private String googleClientId;

    @Value("${auth.google.client-secret:}")
    private String googleClientSecret;

    @Value("${auth.google.redirect-uri:}")
    private String googleRedirectUri;

    @Value("${auth.facebook.app-id:}")
    private String facebookAppId;

    @Value("${auth.facebook.app-secret:}")
    private String facebookAppSecret;

    @Value("${auth.facebook.redirect-uri:}")
    private String facebookRedirectUri;

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

    public String loginWithGoogle(String idTokenString) throws Exception {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "He thong chua cau hinh Google Client ID.");
        }
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu Google ID token.");
        }

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token Google khong hop le.");
        }
        return findOrCreateSocialAccount("GOOGLE", idToken.getPayload().getSubject(), idToken.getPayload().getEmail());
    }

    public String buildGoogleAuthorizationUrl(String state, String redirectUriOverride) {
        String redirectUri = firstNonBlank(redirectUriOverride, googleRedirectUri);
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "He thong chua cau hinh Google Client ID.");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "He thong chua cau hinh Google redirect URI.");
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent");
        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }
        return builder.encode().build().toUriString();
    }

    public String loginWithGoogleAuthCode(String code, String redirectUriOverride) throws Exception {
        String normalizedCode = normalizeText(code);
        String redirectUri = firstNonBlank(redirectUriOverride, googleRedirectUri);

        if (normalizedCode == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu authorization code tu Google.");
        }
        if (googleClientId == null || googleClientId.isBlank() || googleClientSecret == null || googleClientSecret.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "He thong chua cau hinh Google Client ID/Secret.");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "He thong chua cau hinh Google redirect URI.");
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", normalizedCode);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("https://oauth2.googleapis.com/token", requestEntity, Map.class);
        Map<String, Object> tokenResponse = response.getBody();
        if (tokenResponse == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong doi duoc token Google.");
        }

        String idToken = tokenResponse.get("id_token") == null ? null : tokenResponse.get("id_token").toString();
        if (idToken == null || idToken.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Google khong tra ve ID token.");
        }
        return loginWithGoogle(idToken);
    }

    public String loginWithFacebook(String accessToken) throws Exception {
        String normalizedToken = normalizeText(accessToken);
        if (normalizedToken == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu Facebook access token.");
        }

        String fbUrl = UriComponentsBuilder
                .fromUriString("https://graph.facebook.com/v19.0/me")
                .queryParam("fields", "id,name,email")
                .queryParam("access_token", normalizedToken)
                .build()
                .encode()
                .toUriString();
        RestTemplate restTemplate = new RestTemplate();
        try {
            Map<String, Object> userData = restTemplate.getForEntity(fbUrl, Map.class).getBody();
            if (userData == null || userData.get("email") == null) {
                throw new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "Khong lay duoc email tu Facebook. Hay cap quyen email va dung tai khoan co email."
                );
            }
            return findOrCreateSocialAccount("FACEBOOK", stringify(userData.get("id")), (String) userData.get("email"));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Xac thuc Facebook that bai.");
        }
    }

    public String buildFacebookAuthorizationUrl(String state, String redirectUriOverride) {
        String redirectUri = firstNonBlank(redirectUriOverride, facebookRedirectUri);
        if (facebookAppId == null || facebookAppId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "He thong chua cau hinh Facebook App ID.");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "He thong chua cau hinh Facebook redirect URI.");
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://www.facebook.com/v19.0/dialog/oauth")
                .queryParam("client_id", facebookAppId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "email,public_profile");
        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }
        return builder.encode().build().toUriString();
    }

    public String loginWithFacebookAuthCode(String code, String redirectUriOverride) throws Exception {
        String normalizedCode = normalizeText(code);
        String redirectUri = firstNonBlank(redirectUriOverride, facebookRedirectUri);
        if (normalizedCode == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu authorization code tu Facebook.");
        }
        if (facebookAppId == null || facebookAppId.isBlank() || facebookAppSecret == null || facebookAppSecret.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "He thong chua cau hinh Facebook App ID/Secret.");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "He thong chua cau hinh Facebook redirect URI.");
        }

        String exchangeUrl = UriComponentsBuilder
                .fromUriString("https://graph.facebook.com/v19.0/oauth/access_token")
                .queryParam("client_id", facebookAppId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("client_secret", facebookAppSecret)
                .queryParam("code", normalizedCode)
                .build()
                .encode()
                .toUriString();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.getForEntity(exchangeUrl, Map.class);
        Map<String, Object> tokenBody = response.getBody();
        if (tokenBody == null || tokenBody.get("access_token") == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong doi duoc access token Facebook.");
        }
        return loginWithFacebook(tokenBody.get("access_token").toString());
    }

    private String findOrCreateSocialAccount(String provider, String providerUserId, String email) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedProviderUserId = normalizeText(providerUserId);
        String normalizedEmail = normalizeEmail(email);

        if (normalizedProvider == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Provider social khong hop le.");
        }
        if (normalizedProviderUserId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Tai khoan social khong co dinh danh hop le.");
        }
        if (normalizedEmail == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Tai khoan social khong co email hop le.");
        }

        SocialIdentity existingIdentity = socialIdentityRepository
                .findByProviderAndProviderUserId(normalizedProvider, normalizedProviderUserId)
                .orElse(null);
        if (existingIdentity != null) {
            ensureSocialIdentityUsesSameEmail(existingIdentity, normalizedEmail);
            return existingIdentity.getAccount().getUsername();
        }

        Account resolvedAccount = resolveAccountForSocialEmail(normalizedEmail);
        if (resolvedAccount == null) {
            resolvedAccount = createAccount(
                    normalizedEmail,
                    generateRandomSocialPassword(),
                    "ROLE_PATIENT",
                    null,
                    null,
                    normalizedEmail,
                    false
            );
        }

        SocialIdentity identityByEmail = socialIdentityRepository
                .findByProviderAndEmailIgnoreCase(normalizedProvider, normalizedEmail)
                .orElse(null);
        if (identityByEmail != null && !identityByEmail.getAccount().getId().equals(resolvedAccount.getId())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Email social da duoc lien ket voi mot tai khoan khac."
            );
        }

        SocialIdentity socialIdentity = identityByEmail == null ? new SocialIdentity() : identityByEmail;
        socialIdentity.setProvider(normalizedProvider);
        socialIdentity.setProviderUserId(normalizedProviderUserId);
        socialIdentity.setEmail(normalizedEmail);
        socialIdentity.setAccount(resolvedAccount);
        socialIdentityRepository.save(socialIdentity);
        return resolvedAccount.getUsername();
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

    private void ensureSocialIdentityUsesSameEmail(SocialIdentity identity, String normalizedEmail) {
        if (!identity.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Tai khoan social nay dang tra ve email khac voi email da lien ket truoc do."
            );
        }
    }

    private Account resolveAccountForSocialEmail(String normalizedEmail) {
        Account accountByUsername = accountRepository.findByUsername(normalizedEmail).orElse(null);
        Account accountByPatientEmail = patientService.findLinkedAccountByEmail(normalizedEmail);

        if (accountByUsername != null && accountByPatientEmail != null
                && !accountByUsername.getId().equals(accountByPatientEmail.getId())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Email nay dang tro toi hai tai khoan khac nhau. Khong the tu dong lien ket."
            );
        }
        return accountByUsername != null ? accountByUsername : accountByPatientEmail;
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

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = normalizeText(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }
        return normalizeText(second);
    }

    private String normalizeProvider(String provider) {
        String normalized = normalizeText(provider);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String generateRandomSocialPassword() {
        return "SOCIAL-" + UUID.randomUUID();
    }
}
