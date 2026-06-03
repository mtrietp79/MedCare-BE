package com.medcare.clinic_backend.dto.payment;

import java.time.LocalDateTime;

public record AppointmentPaymentReceiptResponse(
        Integer appointmentId,
        String appointmentCode,
        PatientInfo patient,
        BookingInfo booking,
        PaymentInfo payment
) {
    public record PatientInfo(
            String fullName,
            String phone,
            String email
    ) {
    }

    public record BookingInfo(
            String doctorName,
            String specialtyName,
            String serviceName,
            LocalDateTime appointmentDate,
            String appointmentStatus,
            String paymentStatus,
            Double consultationFee
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
