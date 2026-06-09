package com.medcare.clinic_backend.entity;

public final class AppointmentCancellationRequestStatus {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String REFUNDED = "REFUNDED";

    private AppointmentCancellationRequestStatus() {
    }

    public static String toLabel(String status) {
        if (status == null) {
            return "Ch\u1edd x\u1eed l\u00fd";
        }
        return switch (status.trim().toUpperCase()) {
            case APPROVED -> "\u0110\u00e3 duy\u1ec7t";
            case REJECTED -> "T\u1eeb ch\u1ed1i";
            case REFUNDED -> "\u0110\u00e3 ho\u00e0n ti\u1ec1n";
            default -> "Ch\u1edd x\u1eed l\u00fd";
        };
    }
}
