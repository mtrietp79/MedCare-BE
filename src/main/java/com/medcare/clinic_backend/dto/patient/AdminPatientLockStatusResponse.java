package com.medcare.clinic_backend.dto.patient;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminPatientLockStatusResponse {
    private String message;
    private Integer patientId;
    private Integer accountId;
    private Boolean isActive;
}
