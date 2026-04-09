package com.medcare.clinic_backend.dto;

public class AuthRequest {
    private String username;
    private String password;
    private String role; // Chỉ dùng khi đăng ký (VD: ROLE_PATIENT, ROLE_DOCTOR)

    // Getters và Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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