package com.medcare.clinic_backend.dto.servicepackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageBookingResponse {
    private Integer bookingId;
    private String bookingCode;
    private String paymentUrl;
    private String message;
}
