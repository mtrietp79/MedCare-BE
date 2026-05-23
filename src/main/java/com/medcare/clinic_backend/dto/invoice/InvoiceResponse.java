package com.medcare.clinic_backend.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private Integer id;
    private String invoiceCode;
    private Integer recordId;
    private Integer medicalRecordId;
    private String patientName;
    private String patientFullName;
    private String patientPhone;
    private String doctorName;
    private String doctorFullName;
    private Double medicineFee;
    private Double serviceFee;
    private Double totalAmount;
    private Double amount;
    private String status;
    private Boolean canPayOnline;
    private LocalDateTime createdAt;
}
