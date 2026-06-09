package com.medcare.clinic_backend.dto.doctor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorPatientMedicalRecordsResponse {
    private PatientProfile patient;
    private List<RecordItem> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientProfile {
        private Integer id;
        private String fullName;
        private String phone;
        private String email;
        private String gender;
        private LocalDate dateOfBirth;
        private String address;
        private String avatarUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordItem {
        private Integer recordId;
        private Integer appointmentId;
        private String appointmentCode;
        private LocalDateTime recordCreatedAt;
        private LocalDate examDate;
        private String type;
        private String typeCode;
        private String symptoms;
        private String diagnosis;
        private String doctorAdvice;
        private List<MedicineItem> medicines;
        private List<ServiceItem> services;
        private InvoiceInfo invoice;
        private FollowUpAppointmentInfo followUpAppointment;

        @JsonProperty("medicalRecordId")
        public Integer getMedicalRecordId() {
            return recordId;
        }

        @JsonProperty("createdAt")
        public LocalDateTime getCreatedAt() {
            return recordCreatedAt;
        }

        @JsonProperty("appointmentType")
        public String getAppointmentType() {
            return typeCode;
        }

        @JsonProperty("appointmentTypeLabel")
        public String getAppointmentTypeLabel() {
            return type;
        }

        @JsonProperty("appointmentTypeCode")
        public String getAppointmentTypeCode() {
            return typeCode;
        }

        @JsonProperty("followUpAppointmentId")
        public Integer getFollowUpAppointmentId() {
            return followUpAppointment == null ? null : followUpAppointment.getAppointmentId();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicineItem {
        private String name;
        private Integer quantity;
        private String unit;
        private String dosage;
        private String note;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceItem {
        private String name;
        private Double price;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceInfo {
        private Integer id;
        private Double consultationFee;
        private Double medicineTotal;
        private Double serviceTotal;
        private Double totalAmount;
        private String status;

        @JsonProperty("medicineFee")
        public Double getMedicineFee() {
            return medicineTotal;
        }

        @JsonProperty("serviceFee")
        public Double getServiceFee() {
            return serviceTotal;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowUpAppointmentInfo {
        private Integer appointmentId;
        private String appointmentCode;
        private LocalDateTime appointmentDateTime;
        private String type;
        private String typeCode;
        private String status;
        private String statusDisplay;
        private String statusColor;
        private String paymentStatus;
        private Double consultationFee;
        private String note;
        private Integer parentAppointmentId;
        private boolean followUp;

        @JsonProperty("appointmentType")
        public String getAppointmentType() {
            return typeCode;
        }

        @JsonProperty("appointmentTypeLabel")
        public String getAppointmentTypeLabel() {
            return type;
        }

        @JsonProperty("appointmentTypeCode")
        public String getAppointmentTypeCode() {
            return typeCode;
        }

        @JsonProperty("isReExamination")
        public boolean getIsReExamination() {
            return followUp;
        }

        @JsonProperty("originalAppointmentId")
        public Integer getOriginalAppointmentId() {
            return parentAppointmentId;
        }
    }
}
