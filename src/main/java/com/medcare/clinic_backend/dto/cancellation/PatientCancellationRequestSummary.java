package com.medcare.clinic_backend.dto.cancellation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientCancellationRequestSummary {
    private Integer id;
    private String status;
    private String statusLabel;
    private Double refundAmount;
    private LocalDateTime createdAt;
}
