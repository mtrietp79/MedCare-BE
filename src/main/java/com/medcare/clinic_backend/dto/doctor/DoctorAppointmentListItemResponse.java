package com.medcare.clinic_backend.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAppointmentListItemResponse {
    private Integer id;
    private Integer patientId;
    private String patientName;
    private String patientPhone;
    private String patientEmail;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String appointmentTimeLabel;
    private String type;
    private String status;
    private Double consultationFee;
    private String paymentStatus;
    private String followUpNote;
    private Integer parentAppointmentId;
    private boolean canExamine;
}
