package com.medcare.clinic_backend.dto.servicepackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageBookingDetailResponse {
    private Integer id;
    private String bookingCode;
    private PatientInfo patient;
    private PackageInfo servicePackage;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private String note;
    private Double totalAmount;
    private String invoiceCategory;
    private String invoiceCategoryDisplay;
    private String paymentStatus;
    private String status;
    private String invoiceCode;
    private Boolean canPayOnline;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientInfo {
        private Integer id;
        private String fullName;
        private String phone;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageInfo {
        private Integer id;
        private String name;
        private String description;
        private Double price;
        private Integer durationMinutes;
        private String imageUrl;
    }
}
