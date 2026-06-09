package com.medcare.clinic_backend.dto.specialty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpecialtyDeleteCheckResponse {
    private boolean canDelete;
    private long doctorCount;
    private long appointmentCount;
    private long medicalRecordCount;
    private String message;
}
