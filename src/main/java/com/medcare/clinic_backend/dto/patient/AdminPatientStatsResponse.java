package com.medcare.clinic_backend.dto.patient;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminPatientStatsResponse {
    private long totalPatients;
    private long activePatients;
    private long lockedPatients;
    private long newPatientsThisMonth;
}