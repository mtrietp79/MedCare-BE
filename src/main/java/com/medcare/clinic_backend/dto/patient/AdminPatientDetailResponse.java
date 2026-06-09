package com.medcare.clinic_backend.dto.patient;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminPatientDetailResponse {
    private Integer id;
    private Integer accountId;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String address;
    private String avatar;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private AdminPatientDetailStatisticsResponse statistics;
    private List<AdminPatientRecentAppointmentResponse> recentAppointments;
    private List<AdminPatientRecentMedicalRecordResponse> recentMedicalRecords;
}