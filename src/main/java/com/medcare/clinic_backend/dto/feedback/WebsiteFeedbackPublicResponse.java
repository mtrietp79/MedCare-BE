package com.medcare.clinic_backend.dto.feedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteFeedbackPublicResponse {
    private Integer id;
    private String fullName;
    private Integer rating;
    private String comment;
    private LocalDate createdAt;
}
