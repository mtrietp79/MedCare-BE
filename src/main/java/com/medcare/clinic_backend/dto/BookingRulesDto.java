package com.medcare.clinic_backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingRulesDto(
        LocalDateTime serverNow,
        LocalDateTime minBookableAt,
        LocalDate maxBookableDate,
        Integer maxAdvanceDays
) {
}
