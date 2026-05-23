package com.medcare.clinic_backend.dto.doctor;

import lombok.Data;

@Data
public class UpdateDoctorProfileRequest {
    private String fullName;
    private String phone;
    private String address;
    private Integer experienceYears;
    private String bio;
}
