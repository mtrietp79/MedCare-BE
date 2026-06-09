package com.medcare.clinic_backend.dto.specialty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpecialtyDeleteConflictResponse {
    private String code;
    private String message;
    private long doctorCount;
    private long appointmentCount;
    private long medicalRecordCount;
}
