package com.medcare.clinic_backend.dto.cancellation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCancellationRequestListItemResponse {
    private Integer id;
    private Integer appointmentId;
    private String appointmentCode;
    private String patientName;
    private String patientEmail;
    private String doctorName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private Double refundAmount;
    private String cancelReason;
    private String patientNote;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;
    private String status;
    private String statusLabel;
    private String adminNote;
    private LocalDateTime createdAt;
}
