package com.medcare.clinic_backend.dto.feedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorFeedbackResponse {
    private Integer id;
    private String patientName;
    private Integer rating;
    private String comment;
    private LocalDate createdAt;
}
