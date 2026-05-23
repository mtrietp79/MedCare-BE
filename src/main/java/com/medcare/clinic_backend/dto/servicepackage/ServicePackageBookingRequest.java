package com.medcare.clinic_backend.dto.servicepackage;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ServicePackageBookingRequest {
    private Integer packageId;

    @JsonAlias({"appointmentDate"})
    private LocalDate bookingDate;

    @JsonAlias({"appointmentTime"})
    private LocalTime bookingTime;

    private String note;
}
