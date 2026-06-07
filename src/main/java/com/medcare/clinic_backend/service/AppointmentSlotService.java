package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.SlotAvailabilityDto;
import com.medcare.clinic_backend.entity.DoctorSchedule;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentSlotService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;

    public static final int MIN_BOOKING_LEAD_HOURS = 2;
    public static final int MAX_BOOKING_AHEAD_DAYS = 30;

    public List<SlotAvailabilityDto> getDoctorSlots(Integer doctorId, LocalDate date) {
        LocalDateTime serverNow = LocalDateTime.now();
        LocalDateTime minAllowedStart = serverNow.plusHours(MIN_BOOKING_LEAD_HOURS);
        LocalDate maxBookableDate = serverNow.toLocalDate().plusDays(MAX_BOOKING_AHEAD_DAYS);

        boolean hasConfiguredSchedule = doctorScheduleRepository.countByDoctorId(doctorId) > 0;
        Set<String> availableShifts = doctorScheduleRepository.findByDoctorIdAndWorkDate(doctorId, date).stream()
                .map(DoctorSchedule::getShift)
                .map(this::normalizeScheduleShift)
                .collect(Collectors.toSet());

        List<SlotAvailabilityDto> result = new ArrayList<>();
        for (SlotRule slotRule : buildDailySlotRules(date)) {
            long bookedSlots = appointmentRepository.countByDoctorInSlot(doctorId, slotRule.start(), slotRule.end());
            boolean full = bookedSlots >= slotRule.maxPatients();

            String disabledReason = resolveDisabledReason(slotRule.start(), full, serverNow, minAllowedStart, maxBookableDate);
            if (disabledReason == null) {
                disabledReason = resolveDoctorScheduleDisabledReason(hasConfiguredSchedule, availableShifts, slotRule.shift());
            }

            boolean disabled = disabledReason != null;

            result.add(new SlotAvailabilityDto(
                    slotRule.start(),
                    slotRule.end(),
                    slotRule.shift().toLowerCase(Locale.ROOT),
                    slotRule.maxPatients(),
                    bookedSlots,
                    full,
                    disabled,
                    disabledReason
            ));
        }
        return result;
    }

    public void validateSlotAvailability(Integer doctorId, LocalDateTime slotDateTime) {
        LocalDate date = slotDateTime.toLocalDate();
        List<SlotAvailabilityDto> slots = getDoctorSlots(doctorId, date);

        SlotAvailabilityDto targetSlot = slots.stream()
                .filter(s -> s.startTime().equals(slotDateTime))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Khung giờ không hợp lệ: " + slotDateTime.toLocalTime()));

        if (targetSlot.disabled()) {
            String reason = targetSlot.disabledReason();
            if ("FULL".equals(reason)) {
                throw new IllegalStateException("Khung giờ " + slotDateTime.toLocalTime() + " ngày " + date + " đã hết slot. Vui lòng chọn khung giờ khác.");
            } else {
                throw new IllegalStateException("Khung giờ " + slotDateTime.toLocalTime() + " ngày " + date + " không khả dụng (" + reason + ").");
            }
        }
    }

    private String resolveDisabledReason(LocalDateTime slotStart, boolean full, LocalDateTime serverNow, LocalDateTime minAllowedStart, LocalDate maxBookableDate) {
        if (full) return "FULL";
        if (slotStart.isBefore(serverNow)) return "PAST_TIME";
        if (slotStart.isBefore(minAllowedStart)) return "TOO_SOON";
        if (slotStart.toLocalDate().isAfter(maxBookableDate)) return "TOO_FAR_AHEAD";
        return null;
    }

    private String resolveDoctorScheduleDisabledReason(boolean hasConfiguredSchedule, Set<String> availableShifts, String slotShift) {
        if (!hasConfiguredSchedule) return null;
        if (availableShifts.contains(slotShift)) return null;
        return "SHIFT_UNAVAILABLE";
    }

    private String normalizeScheduleShift(String shift) {
        if (shift == null) return "UNKNOWN";
        String s = shift.toUpperCase(Locale.ROOT).trim();
        if (s.contains("SANG") || s.contains("MORNING")) return "MORNING";
        if (s.contains("CHIEU") || s.contains("AFTERNOON")) return "AFTERNOON";
        return s;
    }

    public List<SlotRule> buildDailySlotRules(LocalDate date) {
        return List.of(
                new SlotRule(date, date.atTime(7, 30), date.atTime(8, 0), "MORNING", 3),
                new SlotRule(date, date.atTime(8, 0), date.atTime(9, 0), "MORNING", 5),
                new SlotRule(date, date.atTime(9, 0), date.atTime(10, 0), "MORNING", 5),
                new SlotRule(date, date.atTime(10, 0), date.atTime(11, 0), "MORNING", 5),
                new SlotRule(date, date.atTime(12, 30), date.atTime(13, 0), "AFTERNOON", 3),
                new SlotRule(date, date.atTime(13, 0), date.atTime(14, 0), "AFTERNOON", 5),
                new SlotRule(date, date.atTime(14, 0), date.atTime(15, 0), "AFTERNOON", 5),
                new SlotRule(date, date.atTime(15, 0), date.atTime(16, 0), "AFTERNOON", 5)
        );
    }

    public record SlotRule(LocalDate date, LocalDateTime start, LocalDateTime end, String shift, int maxPatients) {
    }
}
