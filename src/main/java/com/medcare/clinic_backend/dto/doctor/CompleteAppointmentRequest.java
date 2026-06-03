package com.medcare.clinic_backend.dto.doctor;

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
        private String followUpDate;
        private String followUpTime;
        private String note;
    }
}
