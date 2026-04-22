package com.medcare.clinic_backend.dto;

public class SocialLoginRequest {
    private String token; // Đây là idToken mà Zen lấy được từ Google

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}