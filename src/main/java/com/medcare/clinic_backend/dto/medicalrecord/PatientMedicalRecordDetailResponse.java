package com.medcare.clinic_backend.dto.medicalrecord;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientMedicalRecordDetailResponse {
    private Integer recordId;
    private String recordCode;
    private LocalDateTime recordCreatedAt;
    private LocalDate examinationDate;
    private String diagnosis;
    private String doctorAdvice;
    private String treatmentPlan;
    private String prescription;
    private AppointmentInfo appointment;
    private DoctorInfo doctor;
    private List<MedicineItem> medicines;
    private List<ServiceItem> services;
    private InvoiceInfo invoice;
    private FollowUpInfo followUpAppointment;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppointmentInfo {
        private Integer id;
        private String appointmentCode;
        private LocalDateTime appointmentDateTime;
        private String type;
        private String status;
        private String statusDisplay;
        private String statusColor;
        private String symptoms;
        private String note;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorInfo {
        private Integer id;
        private String fullName;
        private String phone;
        private String email;
        private String specialtyName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicineItem {
        private Integer medicineId;
        private String name;
        private String unit;
        private Integer quantity;
        private String dosage;
        private String note;
        private Double unitPrice;
        private Double totalPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceItem {
        private Integer serviceId;
        private String name;
        private Integer quantity;
        private String result;
        private Double unitPrice;
        private Double totalPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceInfo {
        private Integer id;
        private String invoiceCode;
        private String invoiceCategory;
        private String invoiceCategoryDisplay;
        private String status;
        private Double consultationFee;
        private Double medicineFee;
        private Double serviceFee;
        private Double totalAmount;
        private Boolean canPayOnline;
        private LocalDateTime createdAt;
        private LocalDateTime paymentDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowUpInfo {
        private Integer appointmentId;
        private String appointmentCode;
        private LocalDateTime appointmentDateTime;
        private String type;
        private String status;
        private String statusDisplay;
        private String statusColor;
        private String note;
    }
}
