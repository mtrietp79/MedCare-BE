package com.medcare.clinic_backend.dto.payment;

import java.time.LocalDateTime;

public record InvoicePaymentReceiptResponse(
        Integer invoiceId,
        String invoiceCode,
        PatientInfo patient,
        InvoiceInfo invoice,
        PaymentInfo payment
) {
    public record PatientInfo(
            String fullName,
            String phone,
            String email
    ) {
    }

    public record InvoiceInfo(
            Integer recordId,
            String medicalRecordCode,
            Integer appointmentId,
            String appointmentCode,
            String appointmentType,
            String doctorName,
            String specialtyName,
            String serviceName,
            String invoiceCategory,
            String invoiceCategoryDisplay,
            Double consultationFee,
            Double medicineFee,
            Double serviceFee,
            Double totalAmount,
            String invoiceStatus,
            LocalDateTime createdAt
    ) {
    }

    public record PaymentInfo(
            String method,
            String transactionNo,
            String bankCode,
            Double amount,
            LocalDateTime paidAt,
            String responseCode
    ) {
    }
}
