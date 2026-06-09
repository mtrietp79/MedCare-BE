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
public class AdminCancellationRequestDetailResponse {
    private Integer id;
    private Integer appointmentId;
    private String appointmentCode;
    private String appointmentStatus;
    private String appointmentStatusLabel;
    private String paymentStatus;
    private String paymentStatusLabel;
    private Integer patientId;
    private String patientName;
    private String patientEmail;
    private String patientPhone;
    private String doctorName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private Integer invoiceId;
    private Double refundAmount;
    private String cancelReason;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;
    private String patientNote;
    private String status;
    private String statusLabel;
    private String adminNote;
    private Integer processedByAdminId;
    private String processedByAdminUsername;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
