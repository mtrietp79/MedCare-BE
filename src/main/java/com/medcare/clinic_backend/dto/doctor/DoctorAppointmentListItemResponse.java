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
public class DoctorAppointmentListItemResponse {
    private Integer id;
    private String appointmentCode;
    private Integer patientId;
    private String patientName;
    private String patientPhone;
    private String patientEmail;
    private String doctorName;
    private String specialtyName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String appointmentTimeLabel;
    private String type;
    private String typeCode;
    private String status;
    private Double consultationFee;
    private String paymentStatus;
    private String followUpNote;
    private Integer parentAppointmentId;
    private boolean canExamine;
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

    @JsonProperty("canComplete")
    public boolean getCanComplete() {
        return canExamine;
    }
}
