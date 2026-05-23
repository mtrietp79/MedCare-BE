package com.medcare.clinic_backend.dto.servicepackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageBookingListItemResponse {
    private Integer id;
    private String bookingCode;
    private String packageName;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private Double totalAmount;
    private String paymentStatus;
    private String status;
}
