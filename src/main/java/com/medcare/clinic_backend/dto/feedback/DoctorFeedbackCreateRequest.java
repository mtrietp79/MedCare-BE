package com.medcare.clinic_backend.dto.feedback;

import lombok.Data;

@Data
public class DoctorFeedbackCreateRequest {
    private Integer appointmentId;
    private Integer rating;
    private String comment;
}
