package com.medcare.clinic_backend.dto.feedback;

import lombok.Data;

@Data
public class WebsiteFeedbackCreateRequest {
    private String fullName;
    private String email;
    private Integer rating;
    private String comment;
}
