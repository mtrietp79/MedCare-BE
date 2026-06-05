package com.medcare.clinic_backend.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private Integer id;
    private String sourceType;
    private Integer sourceId;
    private String invoiceCode;
    private String invoiceCategory;
    private String invoiceCategoryDisplay;
    private Integer recordId;
    private Integer medicalRecordId;
    private Integer appointmentId;
    private String appointmentCode;
    private String appointmentType;
    private String appointmentTypeDisplay;
    private Integer servicePackageBookingId;
    private String servicePackageBookingCode;
    private String servicePackageName;
    private String patientName;
    private String patientFullName;
    private String patientPhone;
    private String doctorName;
    private String doctorFullName;
    private Double consultationFee;
    private Double medicineFee;
    private Double serviceFee;
    private Double totalAmount;
    private Double amount;
    private String status;
    private String paymentStatus;
    private String paymentStatusDisplay;
    private String bookingStatus;
    private String bookingStatusDisplay;
    private Boolean canPayOnline;
    private LocalDateTime createdAt;
    private LocalDateTime paymentDate;
}
