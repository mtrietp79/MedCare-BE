package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.AuthRequest;
import com.medcare.clinic_backend.dto.AuthResponse;
import com.medcare.clinic_backend.dto.ChangePasswordRequest;
import com.medcare.clinic_backend.dto.ForgotPasswordRequest;
import com.medcare.clinic_backend.dto.ForgotPasswordRequestOtpDto;
import com.medcare.clinic_backend.dto.ResetPasswordRequest;
import com.medcare.clinic_backend.dto.ResetPasswordWithTokenDto;
import com.medcare.clinic_backend.dto.VerifyForgotPasswordOtpDto;
import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.security.JwtTokenProvider;
import com.medcare.clinic_backend.service.AuthService;
import com.medcare.clinic_backend.service.DoctorService;
import com.medcare.clinic_backend.service.PasswordService;
import com.medcare.clinic_backend.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody AuthRequest registerRequest) {
        Account account = new Account();
        account.setUsername(firstNonBlank(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPhone()
        ));
        account.setPassword(registerRequest.getPassword());
        account.setRole("ROLE_PATIENT");

        String result = authService.register(
                account,
                registerRequest.getFullName(),
                registerRequest.getPhone(),
                registerRequest.getEmail()
        );
        return result.toLowerCase().contains("loi")
                ? ResponseEntity.badRequest().body(result)
                : ResponseEntity.ok(result);
    }

    @PostMapping("/register-doctor")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> registerDoctor(@RequestBody AuthRequest registerRequest) {
        String result = authService.registerDoctorAccount(registerRequest.getUsername(), registerRequest.getPassword());
        return result.toLowerCase().contains("loi")
                ? ResponseEntity.badRequest().body(result)
                : ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@RequestBody AuthRequest loginRequest) {
        String identifier = authService.resolveLoginUsername(firstNonBlank(
                loginRequest.getUsername(),
                loginRequest.getEmail(),
                loginRequest.getPhone()
        ));
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, loginRequest.getPassword())
            );
        } catch (DisabledException ex) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        } catch (BadCredentialsException ex) {
            throw ex;
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String username = authentication.getName();
        String role = resolveRole(authentication, username);
        String jwt = tokenProvider.generateToken(username, role);
        Integer id = authService.getAccountIdByUsername(username);
        return ResponseEntity.ok(new AuthResponse(
                id,
                jwt,
                username,
                resolveDisplayName(role, username),
                role,
                resolveProfileCompleted(role, username),
                authService.mustChangePassword(username)
        ));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCurrentAuthInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        String role = resolveRole(authentication, username);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", username);
        result.put("displayName", resolveDisplayName(role, username));
        result.put("role", role);
        result.put("profileCompleted", resolveProfileCompleted(role, username));
        result.put("mustChangePassword", authService.mustChangePassword(username));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/forgot-password/request-otp")
    public ResponseEntity<Map<String, String>> requestForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordRequestOtpDto request
    ) {
        return ResponseEntity.ok(passwordService.requestForgotPasswordOtp(request.getEmail()));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<Map<String, String>> verifyForgotPasswordOtp(
            @Valid @RequestBody VerifyForgotPasswordOtpDto request
    ) {
        return ResponseEntity.ok(passwordService.verifyForgotPasswordOtp(request.getEmail(), request.getOtp()));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, String>> resetPasswordWithToken(
            @Valid @RequestBody ResetPasswordWithTokenDto request
    ) {
        return ResponseEntity.ok(passwordService.resetPasswordWithToken(
                request.getResetToken(),
                request.getNewPassword(),
                request.getConfirmPassword()
        ));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(passwordService.changePassword(
                authentication.getName(),
                request.getOldPassword(),
                request.getNewPassword(),
                request.getConfirmPassword()
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPasswordLegacy(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordService.requestForgotPasswordOtp(request.getUsername()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPasswordLegacy(@RequestBody ResetPasswordRequest request) {
        Map<String, String> verifyResult = passwordService.verifyForgotPasswordOtp(
                request.getUsername(),
                request.getOtp()
        );
        Map<String, String> resetResult = passwordService.resetPasswordWithToken(
                verifyResult.get("resetToken"),
                request.getNewPassword(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(resetResult);
    }

    private String resolveRole(Authentication authentication, String username) {
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .findFirst()
                    .orElseGet(() -> authService.getRoleByUsername(username));
        }
        return authService.getRoleByUsername(username);
    }

    private Boolean resolveProfileCompleted(String role, String username) {
        if (!"ROLE_PATIENT".equals(role)) {
            return null;
        }
        return patientService.isProfileCompletedByUsername(username);
    }

    private String resolveDisplayName(String role, String username) {
        if ("ROLE_PATIENT".equals(role)) {
            return patientService.getDisplayNameByUsername(username);
        }
        if ("ROLE_DOCTOR".equals(role)) {
            return doctorService.getDisplayNameByUsername(username);
        }
        return username;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
