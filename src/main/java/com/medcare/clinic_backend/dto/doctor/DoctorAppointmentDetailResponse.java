package com.medcare.clinic_backend.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAppointmentDetailResponse {
    private Integer id;
    private PatientInfo patient;
    private DoctorInfo doctor;
    private SpecialtyInfo specialty;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String appointmentTimeLabel;
    private String type;
    private String status;
    private String note;
    private String symptoms;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientInfo {
        private Integer id;
        private String fullName;
        private String phone;
        private String email;
        private String gender;
        private LocalDate dateOfBirth;
        private String address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorInfo {
        private Integer id;
        private String fullName;
        private String email;
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecialtyInfo {
        private Integer id;
        private String name;
    }
}
