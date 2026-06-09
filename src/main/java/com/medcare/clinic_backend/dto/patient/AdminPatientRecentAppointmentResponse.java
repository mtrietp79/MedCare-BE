package com.medcare.clinic_backend.dto.patient;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class AdminPatientRecentAppointmentResponse {
    private Integer id;
    private String appointmentCode;
    private String doctorName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String appointmentType;
    private String status;
}