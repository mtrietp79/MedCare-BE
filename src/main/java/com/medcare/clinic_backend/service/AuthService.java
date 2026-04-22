package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.repository.AccountRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender; // Cần thư viện starter-mail trong pom.xml

    private final String GOOGLE_CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID";

    // ==========================================
    // 1. HÀM ĐĂNG KÝ (Bị thiếu đã được thêm lại)
    // ==========================================
    public String register(Account account) {
        // Kiểm tra xem email đã tồn tại chưa
        if (accountRepository.findByUsername(account.getUsername()).isPresent()) {
            return "Lỗi: Email đã tồn tại trên hệ thống!";
        }

        // Mã hóa mật khẩu và lưu vào DB
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        accountRepository.save(account);

        return "Đăng ký thành công!";
    }

    // ==========================================
    // 2. Logic Quên mật khẩu: Tạo mã và gửi Mail
    // ==========================================
    public void processForgotPassword(String email) throws Exception {
        Account account = accountRepository.findByUsername(email)
                .orElseThrow(() -> new Exception("Email không tồn tại trên hệ thống!"));

        String otp = String.format("%06d", new Random().nextInt(999999));
        account.setResetOtp(otp);
        account.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        accountRepository.save(account);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã OTP khôi phục mật khẩu - MedCare");
        message.setText("Mã OTP của bạn là: " + otp + ". Hiệu lực trong 5 phút.");
        mailSender.send(message);
    }

    // ==========================================
    // 3. Logic Đổi mật khẩu mới
    // ==========================================
    public void resetPassword(String email, String otp, String newPassword) throws Exception {
        Account account = accountRepository.findByUsername(email)
                .orElseThrow(() -> new Exception("Tài khoản không tồn tại!"));

        if (account.getResetOtp() == null || !account.getResetOtp().equals(otp)) {
            throw new Exception("Mã OTP không hợp lệ!");
        }

        if (account.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            throw new Exception("Mã OTP đã hết hạn!");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        account.setResetOtp(null);
        account.setOtpExpiryTime(null);
        accountRepository.save(account);
    }

    // ==========================================
    // 4. LOGIC ĐĂNG NHẬP GOOGLE
    // ==========================================
    public String loginWithGoogle(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) throw new Exception("Token Google không hợp lệ!");
        return findOrCreateSocialAccount(idToken.getPayload().getEmail());
    }

    // ==========================================
    // 5. LOGIC ĐĂNG NHẬP FACEBOOK
    // ==========================================
    public String loginWithFacebook(String accessToken) throws Exception {
        String fbUrl = "https://graph.facebook.com/me?fields=id,name,email&access_token=" + accessToken;
        RestTemplate restTemplate = new RestTemplate();
        try {
            Map<String, Object> userData = restTemplate.getForEntity(fbUrl, Map.class).getBody();
            if (userData == null || !userData.containsKey("email")) throw new Exception("Lỗi lấy email FB");
            return findOrCreateSocialAccount((String) userData.get("email"));
        } catch (Exception e) {
            throw new Exception("Xác thực Facebook thất bại!");
        }
    }

    // ==========================================
    // Hàm phụ: Tạo tài khoản nếu đăng nhập MXH lần đầu
    // ==========================================
    private String findOrCreateSocialAccount(String email) {
        Account account = accountRepository.findByUsername(email).orElseGet(() -> {
            // Mật khẩu sẽ được mã hóa bên trong hàm register()
            Account newAcc = new Account(email, "Social@123", "ROLE_PATIENT");
            register(newAcc); // Gọi hàm đăng ký ở trên
            return newAcc;
        });
        return account.getUsername();
    }
}