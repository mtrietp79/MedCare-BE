package com.medcare.clinic_backend.dto.patient;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AdminPatientRecentMedicalRecordResponse {
    private Integer id;
    private String appointmentCode;
    private String doctorName;
    private String diagnosis;
    private LocalDate examDate;
}