package com.medcare.clinic_backend.dto.doctor;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
public class CompleteAppointmentRequest {
    private String symptoms;
    private String diagnosis;
    private String doctorAdvice;
    private List<MedicineItem> medicineItems;
    private List<ServiceItem> serviceItems;
    private FollowUp followUp;
    private Boolean needFollowUp;

    @JsonAlias({"date", "appointmentDate"})
    private String followUpDate;

    @JsonAlias({"time", "appointmentTime"})
    private String followUpTime;

    @JsonAlias({"followUpNote", "note"})
    private String followUpNote;

    @Data
    public static class MedicineItem {
        private Integer medicineId;
        private Integer quantity;
        private String dosage;
        private String note;
    }

    @Data
    public static class ServiceItem {
        private Integer medicalServiceId;
        private String note;
    }

    @Data
    public static class FollowUp {
        private Boolean needFollowUp;

        @JsonAlias({"date", "appointmentDate"})
        private String followUpDate;

        @JsonAlias({"time", "appointmentTime"})
        private String followUpTime;

        @JsonAlias("followUpNote")
        private String note;
    }
}
