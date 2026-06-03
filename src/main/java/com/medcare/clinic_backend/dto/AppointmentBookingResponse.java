package com.medcare.clinic_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentBookingResponse {
    private Integer appointmentId;
    private String appointmentCode;
    private Double amount;
    private String paymentUrl;
    private String message;
}
