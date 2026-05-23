package com.medcare.clinic_backend.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteAppointmentResponse {
    private Integer appointmentId;
    private Integer medicalRecordId;
    private String status;
    private Integer followUpAppointmentId;
}
