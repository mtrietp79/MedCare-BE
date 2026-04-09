package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.AuthRequest;
import com.medcare.clinic_backend.dto.AuthResponse;
import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.repository.AccountRepository;
import com.medcare.clinic_backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    // 1. API ĐĂNG KÝ
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthRequest registerRequest) {
        // Kiểm tra xem username đã tồn tại chưa
        if (accountRepository.existsByUsername(registerRequest.getUsername())) {
            return new ResponseEntity<>("Tên đăng nhập đã có người sử dụng!", HttpStatus.BAD_REQUEST);
        }

        // Tạo tài khoản mới
        Account account = new Account();
        account.setUsername(registerRequest.getUsername());

        // BẮT BUỘC: Mã hóa mật khẩu trước khi lưu vào Database
        account.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        // Gán quyền (Mặc định là ROLE_PATIENT nếu không truyền lên)
        String role = registerRequest.getRole() != null ? registerRequest.getRole() : "ROLE_PATIENT";
        account.setRole(role);

        accountRepository.save(account);

        return new ResponseEntity<>("Đăng ký tài khoản thành công!", HttpStatus.OK);
    }

    // 2. API ĐĂNG NHẬP
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@RequestBody AuthRequest loginRequest) {
        // Xác thực username và password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Nếu không văng lỗi tức là tài khoản/mật khẩu đúng -> Set thông tin vào Security Context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Tạo chuỗi JWT Token
        String jwt = tokenProvider.generateToken(authentication.getName());

        // Trả Token về cho người dùng
        return ResponseEntity.ok(new AuthResponse(jwt));
    }
}