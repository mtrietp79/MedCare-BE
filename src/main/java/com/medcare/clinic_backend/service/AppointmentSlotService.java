package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.AppointmentSlotResponse;
import com.medcare.clinic_backend.dto.SlotAvailabilityDto;
import com.medcare.clinic_backend.entity.DoctorSchedule;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter SLOT_ERROR_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<AppointmentSlotResponse> getDoctorSlots(Integer doctorId, LocalDate date) {
        if (doctorId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu doctorId.");
        }
        if (date == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu ngay can kiem tra slot.");
        }

        List<AppointmentSlotResponse> result = new ArrayList<>();
        for (SlotRule slotRule : buildDailySlotRules(date)) {
            long bookedSlots = countBookedSlots(doctorId, slotRule.start());
            int totalSlots = slotRule.maxPatients();
            long remainingSlots = Math.max(0, totalSlots - bookedSlots);
            boolean available = remainingSlots > 0;

            result.add(new AppointmentSlotResponse(
                    slotRule.start().toLocalTime().format(TIME_FORMATTER),
                    totalSlots,
                    bookedSlots,
                    remainingSlots,
                    available
            ));
        }
        return result;
    }

    public List<SlotAvailabilityDto> getDoctorSlotsForPatientBooking(Integer doctorId, LocalDate date) {
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
            long bookedSlots = countBookedSlots(doctorId, slotRule.start());
            int totalSlots = slotRule.maxPatients();
            boolean full = bookedSlots >= totalSlots;

            String disabledReason = resolvePatientDisabledReason(
                    slotRule.start(),
                    full,
                    serverNow,
                    minAllowedStart,
                    maxBookableDate
            );
            if (disabledReason == null) {
                disabledReason = resolveDoctorScheduleDisabledReason(hasConfiguredSchedule, availableShifts, slotRule.shift());
            }

            boolean disabled = disabledReason != null;

            result.add(new SlotAvailabilityDto(
                    slotRule.start(),
                    slotRule.end(),
                    slotRule.shift().toLowerCase(Locale.ROOT),
                    totalSlots,
                    bookedSlots,
                    full,
                    disabled,
                    disabledReason
            ));
        }
        return result;
    }

    public List<AppointmentSlotResponse> getFollowUpSlotsForDoctor(Integer doctorId, LocalDate date) {
        LocalDateTime serverNow = LocalDateTime.now();
        return getDoctorSlots(doctorId, date).stream()
                .map(slot -> {
                    LocalDateTime slotStart = LocalDateTime.of(date, java.time.LocalTime.parse(slot.time()));
                    if (slotStart.isBefore(serverNow)) {
                        return new AppointmentSlotResponse(
                                slot.time(),
                                slot.totalSlots(),
                                slot.bookedSlots(),
                                slot.remainingSlots(),
                                false
                        );
                    }
                    return slot;
                })
                .toList();
    }

    public long countBookedSlots(Integer doctorId, LocalDateTime slotDateTime) {
        LocalDateTime normalized = normalizeSlotDateTime(slotDateTime);
        return appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(doctorId, normalized);
    }

    public void validateSlotAvailability(Integer doctorId, LocalDateTime slotDateTime) {
        LocalDateTime normalized = normalizeSlotDateTime(slotDateTime);
        findSlotRule(normalized);

        if (!hasRemainingSlot(doctorId, normalized)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    buildSlotFullMessage(normalized)
            );
        }
    }

    public void validateFollowUpSlotAvailability(Integer doctorId, LocalDateTime slotDateTime) {
        LocalDateTime normalized = normalizeSlotDateTime(slotDateTime);
        findSlotRule(normalized);

        if (normalized.isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thoi gian tai kham khong duoc o qua khu.");
        }

        if (!hasRemainingSlot(doctorId, normalized)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    buildSlotFullMessage(normalized)
            );
        }
    }

    public SlotRule findSlotRule(LocalDateTime slotDateTime) {
        LocalDateTime normalized = normalizeSlotDateTime(slotDateTime);
        for (SlotRule slotRule : buildDailySlotRules(normalized.toLocalDate())) {
            if (slotRule.start().equals(normalized)) {
                return slotRule;
            }
        }
        throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "Khung gio khong hop le. Chi ho tro 07:30, 08:00, 09:00, 10:00, 12:30, 13:00, 14:00, 15:00."
        );
    }

    public boolean hasRemainingSlot(Integer doctorId, LocalDateTime slotDateTime) {
        LocalDateTime normalized = normalizeSlotDateTime(slotDateTime);
        SlotRule slotRule = findSlotRule(normalized);
        long bookedSlots = countBookedSlots(doctorId, normalized);
        return bookedSlots < slotRule.maxPatients();
    }

    public String buildSlotFullMessage(LocalDateTime slotDateTime) {
        LocalDateTime normalized = normalizeSlotDateTime(slotDateTime);
        return "Khung gio "
                + normalized.toLocalTime().format(TIME_FORMATTER)
                + " ngay "
                + normalized.toLocalDate().format(SLOT_ERROR_DATE_FORMATTER)
                + " da het slot. Vui long chon khung gio khac.";
    }

    private LocalDateTime normalizeSlotDateTime(LocalDateTime slotDateTime) {
        if (slotDateTime == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu thoi gian slot.");
        }
        return slotDateTime.withSecond(0).withNano(0);
    }

    private String resolvePatientDisabledReason(
            LocalDateTime slotStart,
            boolean full,
            LocalDateTime serverNow,
            LocalDateTime minAllowedStart,
            LocalDate maxBookableDate
    ) {
        if (full) {
            return "FULL";
        }
        if (slotStart.isBefore(serverNow)) {
            return "PAST_TIME";
        }
        if (slotStart.isBefore(minAllowedStart)) {
            return "TOO_SOON";
        }
        if (slotStart.toLocalDate().isAfter(maxBookableDate)) {
            return "TOO_FAR_AHEAD";
        }
        return null;
    }

    private String resolveDoctorScheduleDisabledReason(boolean hasConfiguredSchedule, Set<String> availableShifts, String slotShift) {
        if (!hasConfiguredSchedule) {
            return null;
        }
        if (availableShifts == null || availableShifts.isEmpty()) {
            return "NO_SCHEDULE";
        }
        String normalizedShift = normalizeScheduleShift(slotShift);
        if (availableShifts.contains("ALL_DAY") || availableShifts.contains(normalizedShift)) {
            return null;
        }
        return "SHIFT_UNAVAILABLE";
    }

    private String normalizeScheduleShift(String shift) {
        if (shift == null) {
            return "UNKNOWN";
        }
        String normalized = shift.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("SANG") || normalized.contains("MORNING")) {
            return "MORNING";
        }
        if (normalized.contains("CHIEU") || normalized.contains("AFTERNOON")) {
            return "AFTERNOON";
        }
        return normalized;
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
