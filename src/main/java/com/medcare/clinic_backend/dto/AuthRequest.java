package com.medcare.clinic_backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;

public class AuthRequest {
    @JsonAlias({"identifier", "login", "userName"})
    private String username;
    @JsonAlias({"full_name", "name"})
    private String fullName;
    @JsonAlias({"mail"})
    @Email(message = "Email không hợp lệ")
    private String email;
    @JsonAlias({"phoneNumber", "phone_number", "mobile"})
    private String phone;
    @JsonAlias({"pass"})
    private String password;
    private String role; // Chỉ dùng khi đăng ký (VD: ROLE_PATIENT, ROLE_DOCTOR)

    // Getters và Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
