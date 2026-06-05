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
public class CompleteAppointmentResponse {
    private String message;
    private Integer appointmentId;
    private String appointmentType;
    private String status;
    private InvoiceInfo invoice;
    private FollowUpAppointmentInfo followUpAppointment;

    // Backward-compatible fields for older FE mapping.
    @JsonProperty("invoiceId")
    public Integer getInvoiceId() {
        return invoice == null ? null : invoice.getId();
    }

    @JsonProperty("medicineFee")
    public Double getMedicineFee() {
        return invoice == null ? 0.0 : invoice.getMedicineTotal();
    }

    @JsonProperty("serviceFee")
    public Double getServiceFee() {
        return invoice == null ? 0.0 : invoice.getServiceTotal();
    }

    @JsonProperty("totalAmount")
    public Double getTotalAmount() {
        return invoice == null ? 0.0 : invoice.getTotalAmount();
    }

    @JsonProperty("invoiceCreated")
    public Boolean getInvoiceCreated() {
        return invoice != null;
    }

    @JsonProperty("followUpAppointmentId")
    public Integer getFollowUpAppointmentId() {
        return followUpAppointment == null ? null : followUpAppointment.getId();
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

        // Backward-compatible keys for old FE mapping.
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
        private Integer id;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;
        private String type;
        private String status;
        private Double consultationFee;
        private String paymentStatus;
        private String note;
        private Integer parentAppointmentId;

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
}
