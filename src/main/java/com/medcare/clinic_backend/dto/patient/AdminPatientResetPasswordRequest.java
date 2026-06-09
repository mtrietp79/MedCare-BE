package com.medcare.clinic_backend.dto.patient;

import lombok.Data;

@Data
public class AdminPatientResetPasswordRequest {
    private String temporaryPassword;
}