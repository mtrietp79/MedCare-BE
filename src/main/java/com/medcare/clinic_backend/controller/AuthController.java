package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.AuthRequest;
import com.medcare.clinic_backend.dto.AuthResponse;
import com.medcare.clinic_backend.dto.ForgotPasswordRequest;
import com.medcare.clinic_backend.dto.ResetPasswordRequest;
import com.medcare.clinic_backend.dto.SocialLoginRequest;
import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.security.JwtTokenProvider;
import com.medcare.clinic_backend.service.AuthService;
import com.medcare.clinic_backend.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

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

    @Autowired
    private PatientService patientService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthRequest registerRequest) {
        Account account = new Account();
        account.setUsername(registerRequest.getUsername());
        account.setPassword(registerRequest.getPassword());
        account.setRole("ROLE_PATIENT");

        String result = authService.register(account);
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
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String username = authentication.getName();
        String role = resolveRole(authentication, username);
        String jwt = tokenProvider.generateToken(username);
        return ResponseEntity.ok(new AuthResponse(jwt, username, role, resolveProfileCompleted(role, username)));
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody SocialLoginRequest request) {
        try {
            String username = authService.loginWithGoogle(request.getToken());
            String role = authService.getRoleByUsername(username);
            return ResponseEntity.ok(new AuthResponse(
                    tokenProvider.generateToken(username),
                    username,
                    role,
                    resolveProfileCompleted(role, username)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/facebook")
    public ResponseEntity<?> facebookLogin(@RequestBody SocialLoginRequest request) {
        try {
            String username = authService.loginWithFacebook(request.getToken());
            String role = authService.getRoleByUsername(username);
            return ResponseEntity.ok(new AuthResponse(
                    tokenProvider.generateToken(username),
                    username,
                    role,
                    resolveProfileCompleted(role, username)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCurrentAuthInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        String role = resolveRole(authentication, username);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", username);
        result.put("role", role);
        result.put("profileCompleted", resolveProfileCompleted(role, username));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            return ResponseEntity.ok(authService.processForgotPassword(request.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getUsername(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok("Dat lai mat khau thanh cong.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
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
}
