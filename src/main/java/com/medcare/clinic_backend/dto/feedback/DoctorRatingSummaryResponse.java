package com.medcare.clinic_backend.dto.feedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRatingSummaryResponse {
    private Integer doctorId;
    private Double averageRating;
    private Long totalFeedbacks;
}
