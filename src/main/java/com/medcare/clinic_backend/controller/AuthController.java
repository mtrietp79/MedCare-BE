package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.AuthRequest;
import com.medcare.clinic_backend.dto.AuthResponse;
import com.medcare.clinic_backend.dto.ForgotPasswordRequest;
import com.medcare.clinic_backend.dto.ResetPasswordRequest;
import com.medcare.clinic_backend.dto.SocialCodeLoginRequest;
import com.medcare.clinic_backend.dto.SocialLoginRequest;
import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.security.JwtTokenProvider;
import com.medcare.clinic_backend.service.AuthService;
import com.medcare.clinic_backend.service.DoctorService;
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

    @Autowired
    private DoctorService doctorService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthRequest registerRequest) {
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
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String username = authentication.getName();
        String role = resolveRole(authentication, username);
        String jwt = tokenProvider.generateToken(username);
        return ResponseEntity.ok(new AuthResponse(
                jwt,
                username,
                resolveDisplayName(role, username),
                role,
                resolveProfileCompleted(role, username)
        ));
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody SocialLoginRequest request) {
        try {
            String username = authService.loginWithGoogle(request.getToken());
            return ResponseEntity.ok(buildAuthResponse(username));
        } catch (BusinessException e) {
            return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Da xay ra loi he thong. Vui long thu lai sau."));
        }
    }

    @GetMapping("/google/url")
    public ResponseEntity<?> googleAuthUrl(@RequestParam(required = false) String state,
                                           @RequestParam(required = false) String redirectUri) {
        return ResponseEntity.ok(Map.of("url", authService.buildGoogleAuthorizationUrl(state, redirectUri)));
    }

    @PostMapping("/google/code")
    public ResponseEntity<?> googleCodeLogin(@RequestBody SocialCodeLoginRequest request) {
        try {
            String username = authService.loginWithGoogleAuthCode(request.getCode(), request.getRedirectUri());
            return ResponseEntity.ok(buildAuthResponse(username));
        } catch (BusinessException e) {
            return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Da xay ra loi he thong. Vui long thu lai sau."));
        }
    }

    @PostMapping("/facebook")
    public ResponseEntity<?> facebookLogin(@RequestBody SocialLoginRequest request) {
        try {
            String username = authService.loginWithFacebook(request.getToken());
            return ResponseEntity.ok(buildAuthResponse(username));
        } catch (BusinessException e) {
            return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Da xay ra loi he thong. Vui long thu lai sau."));
        }
    }

    @GetMapping("/facebook/url")
    public ResponseEntity<?> facebookAuthUrl(@RequestParam(required = false) String state,
                                             @RequestParam(required = false) String redirectUri) {
        return ResponseEntity.ok(Map.of("url", authService.buildFacebookAuthorizationUrl(state, redirectUri)));
    }

    @PostMapping("/facebook/code")
    public ResponseEntity<?> facebookCodeLogin(@RequestBody SocialCodeLoginRequest request) {
        try {
            String username = authService.loginWithFacebookAuthCode(request.getCode(), request.getRedirectUri());
            return ResponseEntity.ok(buildAuthResponse(username));
        } catch (BusinessException e) {
            return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Da xay ra loi he thong. Vui long thu lai sau."));
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
        result.put("displayName", resolveDisplayName(role, username));
        result.put("role", role);
        result.put("profileCompleted", resolveProfileCompleted(role, username));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.processForgotPassword(request.getUsername()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getUsername(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok("Dat lai mat khau thanh cong.");
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

    private AuthResponse buildAuthResponse(String username) {
        String role = authService.getRoleByUsername(username);
        return new AuthResponse(
                tokenProvider.generateToken(username),
                username,
                resolveDisplayName(role, username),
                role,
                resolveProfileCompleted(role, username)
        );
    }
}
