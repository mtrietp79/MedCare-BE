package com.medcare.clinic_backend.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFollowUpResponse {
    private Integer appointmentId;
    private Integer patientId;
    private Integer doctorId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String type;
    private String status;
    private Double consultationFee;
    private String paymentStatus;
    private String note;
    private Integer parentAppointmentId;
}
