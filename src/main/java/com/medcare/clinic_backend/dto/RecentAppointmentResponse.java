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

    public String getPatient() {
        return patientName;
    }

    public String getDoctor() {
        return doctorName;
    }

    public String getSpecialty() {
        return specialtyName;
    }

    public String getAppointmentDate() {
        return date;
    }

    public String getAppointmentTime() {
        return time;
    }

    public String getStatusDisplay() {
        return status;
    }
}
