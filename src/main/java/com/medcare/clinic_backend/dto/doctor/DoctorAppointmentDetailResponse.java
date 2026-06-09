package com.medcare.clinic_backend.dto.doctor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAppointmentDetailResponse {
    private Integer id;
    private String appointmentCode;
    private PatientInfo patient;
    private DoctorInfo doctor;
    private SpecialtyInfo specialty;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String appointmentTimeLabel;
    private String type;
    private String typeCode;
    private String status;
    private String paymentStatus;
    private Double consultationFee;
    private String note;
    private String symptoms;
    private String followUpNote;
    private Integer parentAppointmentId;
    private boolean followUp;
    private boolean canExamine;

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

    @JsonProperty("canComplete")
    public boolean getCanComplete() {
        return canExamine;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientInfo {
        private Integer id;
        private String fullName;
        private String phone;
        private String email;
        private String gender;
        private LocalDate dateOfBirth;
        private String address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorInfo {
        private Integer id;
        private String fullName;
        private String email;
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecialtyInfo {
        private Integer id;
        private String name;
    }
}
