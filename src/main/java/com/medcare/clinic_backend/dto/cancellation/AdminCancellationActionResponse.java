package com.medcare.clinic_backend.dto.cancellation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCancellationActionResponse {
    private String message;
    private String status;
}
