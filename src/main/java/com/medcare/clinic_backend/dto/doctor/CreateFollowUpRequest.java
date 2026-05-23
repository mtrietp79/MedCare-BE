package com.medcare.clinic_backend.dto.doctor;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateFollowUpRequest {
    private LocalDate followUpDate;
    private LocalTime followUpTime;
    private String note;
}
