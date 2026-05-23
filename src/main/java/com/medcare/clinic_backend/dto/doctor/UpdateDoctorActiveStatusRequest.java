package com.medcare.clinic_backend.dto.doctor;

import lombok.Data;

@Data
public class UpdateDoctorActiveStatusRequest {
    private Boolean active;
    private String status;
}
