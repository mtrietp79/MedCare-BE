package com.medcare.clinic_backend.dto.servicepackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminServicePackageBookingResponse {
    private Integer id;
    private String bookingCode;
    private String patientName;
    private String patientPhone;
    private String packageName;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private Double amount;
    private Double paidAmount;
    private String paymentStatus;
    private String status;
    private LocalDateTime createdAt;
}
