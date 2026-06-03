package com.medcare.clinic_backend.dto.payment;

public record PaymentReturnResult(
        boolean success,
        String message,
        String responseCode
) {
}
