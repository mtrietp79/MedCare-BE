package com.medcare.clinic_backend.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorScheduleDayAppointmentResponse {
    private Integer appointmentId;
    private String patientName;
    private LocalTime time;
    private String timeLabel;
    private String type;
    private String status;
}
