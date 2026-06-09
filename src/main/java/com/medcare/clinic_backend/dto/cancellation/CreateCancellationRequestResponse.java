package com.medcare.clinic_backend.dto.cancellation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCancellationRequestResponse {
    private String message;
    private Integer requestId;
    private Integer appointmentId;
    private String status;
}
