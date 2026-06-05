package com.medcare.clinic_backend.dto.payment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ServicePackagePaymentReceiptResponse(
        Integer bookingId,
        String bookingCode,
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
            String packageName,
            String packageDescription,
            LocalDate bookingDate,
            LocalTime bookingTime,
            String bookingStatus,
            String paymentStatus,
            Double totalAmount,
            String note
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
