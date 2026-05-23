package com.medcare.clinic_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentAppointmentResponse {
    private String patientName;
    private String doctorName;
    private String specialtyName;
    private String date;
    private String time;
    private String status;
    private String statusCode;
    private String appointmentDateTime;
}
