package com.medcare.clinic_backend.dto;

import java.time.LocalDateTime;

public record SlotAvailabilityDto(
        LocalDateTime startTime,
        LocalDateTime endTime,
        String shift,
        int maxPatients,
        long bookedPatients,
        boolean full,
        boolean disabled
) {
}
