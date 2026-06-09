package com.medcare.clinic_backend.dto.patient;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminPatientListItemResponse {
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
    private long appointmentCount;
    private long medicalRecordCount;
    private long invoiceCount;
}