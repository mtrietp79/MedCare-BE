package com.medcare.clinic_backend.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorMedicalRecordsSummaryResponse {
    private long totalPatients;
    private long newPatients;
    private long followUpPatients;
}
