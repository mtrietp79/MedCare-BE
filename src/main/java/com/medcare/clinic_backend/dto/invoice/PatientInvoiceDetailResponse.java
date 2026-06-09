package com.medcare.clinic_backend.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientInvoiceDetailResponse {
    private String uniqueKey;
    private Integer id;
    private String invoiceCode;
    private String invoiceType;
    private String invoiceTypeLabel;
    private String sourceType;
    private Integer sourceId;
    private String referenceCode;
    private String appointmentCode;
    private Integer appointmentId;
    private Integer medicalRecordId;
    private String medicalRecordCode;
    private String examType;
    private String appointmentType;
    private String appointmentTypeLabel;
    private Boolean isReExamination;
    private String patientName;
    private String doctorName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private Double consultationFee;
    private Double medicineTotal;
    private Double serviceTotal;
    private Double totalAmount;
    private String paymentStatus;
    private String statusLabel;
    private String bookingStatus;
    private String bookingStatusDisplay;
    private Boolean canPayOnline;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private String packageBookingCode;
    private String servicePackageName;
    private List<PrescriptionItem> prescriptionItems;
    private List<MedicalServiceItem> medicalServiceItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriptionItem {
        private String medicineName;
        private Integer quantity;
        private String unit;
        private String dosage;
        private String note;
        private Double unitPrice;
        private Double totalPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicalServiceItem {
        private String serviceName;
        private Integer quantity;
        private String note;
        private Double unitPrice;
        private Double totalPrice;
    }
}
