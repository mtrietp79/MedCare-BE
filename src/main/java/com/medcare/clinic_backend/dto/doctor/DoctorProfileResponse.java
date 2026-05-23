package com.medcare.clinic_backend.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfileResponse {
    private Integer id;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private Integer specialtyId;
    private String specialtyName;
    private Integer experienceYears;
    private String bio;
    private String avatarUrl;
    private LocalDate createdAt;
    private double rating;
    private String workplace;
}
