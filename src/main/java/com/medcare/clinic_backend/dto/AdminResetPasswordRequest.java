package com.medcare.clinic_backend.dto;

public class AdminResetPasswordRequest {

    private String temporaryPassword;

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }
}
