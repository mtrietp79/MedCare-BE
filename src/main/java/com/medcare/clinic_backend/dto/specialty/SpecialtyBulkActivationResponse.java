package com.medcare.clinic_backend.dto.specialty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpecialtyBulkActivationResponse {
    private String message;
    private int updatedCount;
}
