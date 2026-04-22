package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.AuthRequest;
import com.medcare.clinic_backend.dto.AuthResponse;
import com.medcare.clinic_backend.dto.ForgotPasswordRequest;
import com.medcare.clinic_backend.dto.ResetPasswordRequest;
import com.medcare.clinic_backend.dto.SocialLoginRequest;
import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.security.JwtTokenProvider;
import com.medcare.clinic_backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AuthService authService;

    // 1. API ĐĂNG KÝ
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthRequest registerRequest) {
        Account account = new Account();
        account.setUsername(registerRequest.getUsername());
        account.setPassword(registerRequest.getPassword());

        // Luôn mặc định là ROLE_PATIENT khi đăng ký
        account.setRole("ROLE_PATIENT");

        String result = authService.register(account);
        return result.contains("Lỗi") ?
                ResponseEntity.badRequest().body(result) : ResponseEntity.ok(result);
    }

    // 2. API ĐĂNG NHẬP THƯỜNG
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@RequestBody AuthRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication.getName());
        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    // 3. API ĐĂNG NHẬP GOOGLE
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody SocialLoginRequest request) {
        try {
            String username = authService.loginWithGoogle(request.getToken());
            return ResponseEntity.ok(new AuthResponse(tokenProvider.generateToken(username)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // 4. API ĐĂNG NHẬP FACEBOOK
    @PostMapping("/facebook")
    public ResponseEntity<?> facebookLogin(@RequestBody SocialLoginRequest request) {
        try {
            String username = authService.loginWithFacebook(request.getToken());
            return ResponseEntity.ok(new AuthResponse(tokenProvider.generateToken(username)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // 5. API YÊU CẦU GỬI MÃ OTP QUÊN MẬT KHẨU
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            authService.processForgotPassword(request.getEmail());
            return ResponseEntity.ok("Mã OTP đã được gửi đến email của bạn!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // 6. API XÁC NHẬN OTP VÀ ĐẶT LẠI MẬT KHẨU
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok("Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}