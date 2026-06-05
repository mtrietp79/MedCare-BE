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
public class CreateFollowUpResponse {
    private Integer id;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String type;
    private String status;
    private String paymentStatus;
    private Double consultationFee;

    @JsonProperty("appointmentType")
    public String getAppointmentType() {
        return type;
    }

    @JsonProperty("typeCode")
    public String getTypeCode() {
        return "T\u00e1i kh\u00e1m".equals(type) ? "FOLLOW_UP" : "NEW_EXAM";
    }

    @JsonProperty("appointmentTypeCode")
    public String getAppointmentTypeCode() {
        return getTypeCode();
    }
}
