package com.medcare.clinic_backend.dto;

public record AppointmentSlotResponse(
        String time,
        int totalSlots,
        long bookedSlots,
        long remainingSlots,
        boolean available
) {
}
