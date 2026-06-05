package com.medcare.clinic_backend.dto.doctor;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CreateFollowUpRequest {
    @JsonAlias({"date", "appointmentDate"})
    private String followUpDate;

    @JsonAlias({"time", "appointmentTime"})
    private String followUpTime;

    @JsonAlias("followUpNote")
    private String note;
}
