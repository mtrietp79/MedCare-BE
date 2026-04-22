package com.medcare.clinic_backend.dto;

public class ForgotPasswordRequest {
    private String email;

    // Constructor
    public ForgotPasswordRequest() {}

    public ForgotPasswordRequest(String email) {
        this.email = email;
    }

    // Getters và Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}