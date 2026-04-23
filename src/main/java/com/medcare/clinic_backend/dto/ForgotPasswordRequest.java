package com.medcare.clinic_backend.dto;

public class ForgotPasswordRequest {
    private String username;

    public ForgotPasswordRequest() {}

    public ForgotPasswordRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
