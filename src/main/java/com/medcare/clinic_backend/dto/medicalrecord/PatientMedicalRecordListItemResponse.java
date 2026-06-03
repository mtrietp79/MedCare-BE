package com.medcare.clinic_backend.dto.medicalrecord;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientMedicalRecordListItemResponse {
    private Integer recordId;
    private String recordCode;
    private LocalDateTime recordCreatedAt;
    private Integer appointmentId;
    private String appointmentCode;
    private LocalDate examinationDate;
    private LocalDateTime appointmentDateTime;
    private String appointmentType;
    private String appointmentStatus;
    private String appointmentStatusDisplay;
    private String appointmentStatusColor;
    private Integer doctorId;
    private String doctorName;
    private String specialtyName;
    private String diagnosis;
    private InvoiceSummary invoice;

    @JsonProperty("createdAt")
    public LocalDateTime getCreatedAt() {
        return recordCreatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceSummary {
        private Integer invoiceId;
        private String invoiceCode;
        private String status;
        private Double totalAmount;
    }
}
