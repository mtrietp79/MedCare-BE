package com.medcare.clinic_backend.dto.doctor;

import lombok.Data;

@Data
public class CreateFollowUpRequest {
    private String followUpDate;
    private String followUpTime;
    private String note;
}
