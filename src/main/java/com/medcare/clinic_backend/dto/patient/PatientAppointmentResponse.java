package com.medcare.clinic_backend.dto.patient;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String status;
    private Double consultationFee;
    private String paymentStatus;
    private Integer parentAppointmentId;
    private String followUpNote;

    @JsonProperty("type")
    public String getType() {
        return appointmentType;
    }

    @JsonProperty("typeCode")
    public String getTypeCode() {
        return "T\u00e1i kh\u00e1m".equals(appointmentType) ? "FOLLOW_UP" : "NEW_EXAM";
    }

    @JsonProperty("appointmentTypeCode")
    public String getAppointmentTypeCode() {
        return getTypeCode();
    }
}
