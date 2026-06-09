package com.medcare.clinic_backend.dto.patient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medcare.clinic_backend.dto.cancellation.PatientCancellationRequestSummary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientAppointmentResponse {
    private Integer id;
    private String appointmentCode;
    private String doctorName;
    private String specialtyName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String appointmentTimeLabel;
    private String appointmentType;
    private String appointmentTypeLabel;
    private Boolean isReExamination;
    private String status;
    private String statusLabel;
    private Double consultationFee;
    private String paymentStatus;
    private String paymentStatusLabel;
    private Integer parentAppointmentId;
    private String followUpNote;
    private PatientCancellationRequestSummary cancellationRequest;
    private Boolean isCancelled;
    private Boolean hasCancellationRequest;

    @JsonProperty("type")
    public String getType() {
        return appointmentTypeLabel;
    }

    @JsonProperty("typeCode")
    public String getTypeCode() {
        return appointmentType;
    }

    @JsonProperty("appointmentTypeCode")
    public String getAppointmentTypeCode() {
        return appointmentType;
    }

    @JsonProperty("originalAppointmentId")
    public Integer getOriginalAppointmentId() {
        return parentAppointmentId;
    }
}
