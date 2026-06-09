package com.medcare.clinic_backend.dto.doctor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorScheduleDayAppointmentResponse {
    private Integer appointmentId;
    private String patientName;
    private LocalTime time;
    private String timeLabel;
    private String type;
    private String typeCode;
    private String status;
    private boolean followUp;
    private Integer parentAppointmentId;

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
