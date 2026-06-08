package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.AppointmentSlotResponse;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentSlotServiceTest {

    private static final Integer DOCTOR_ID = 7;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @InjectMocks
    private AppointmentSlotService appointmentSlotService;

    @Test
    void getDoctorSlots_shouldReturnDefaultCapacityWhenDatabaseIsEmpty() {
        LocalDate date = LocalDate.of(2026, 6, 12);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 7, 30)))).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 8, 0)))).thenReturn(4L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 9, 0)))).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 10, 0)))).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 12, 30)))).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 13, 0)))).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 14, 0)))).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 15, 0)))).thenReturn(0L);

        List<AppointmentSlotResponse> slots = appointmentSlotService.getDoctorSlots(DOCTOR_ID, date);

        AppointmentSlotResponse slot730 = findSlot(slots, "07:30");
        assertEquals(3, slot730.getTotalSlots());
        assertEquals(0, slot730.getBookedSlots());
        assertEquals(3, slot730.getRemainingSlots());
        assertTrue(slot730.isAvailable());

        AppointmentSlotResponse slot800 = findSlot(slots, "08:00");
        assertEquals(5, slot800.getTotalSlots());
        assertEquals(4, slot800.getBookedSlots());
        assertEquals(1, slot800.getRemainingSlots());
        assertTrue(slot800.isAvailable());
        assertEquals(5, slot800.getMaxPatients());
        assertEquals(4, slot800.getBookedPatients());
        assertFalse(slot800.isFull());
        assertFalse(slot800.isDisabled());
    }

    @Test
    void getDoctorSlots_shouldMarkFullSlotAsUnavailable() {
        LocalDate date = LocalDate.of(2026, 6, 12);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 7, 30)))).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 8, 0)))).thenReturn(5L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 9, 0)))).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 10, 0)))).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 12, 30)))).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 13, 0)))).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 14, 0)))).thenReturn(0L);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(eq(DOCTOR_ID), eq(anySlot(date, 15, 0)))).thenReturn(0L);

        List<AppointmentSlotResponse> slots = appointmentSlotService.getDoctorSlots(DOCTOR_ID, date);

        AppointmentSlotResponse slot800 = findSlot(slots, "08:00");
        assertEquals(5, slot800.getBookedSlots());
        assertEquals(0, slot800.getRemainingSlots());
        assertFalse(slot800.isAvailable());
        assertTrue(slot800.isFull());
        assertTrue(slot800.isDisabled());
        assertEquals("FULL", slot800.getDisabledReason());
    }

    @Test
    void validateSlotAvailability_shouldRejectWhenSlotIsFull() {
        LocalDate date = LocalDate.of(2026, 6, 12);
        LocalDateTime slotDateTime = date.atTime(8, 0);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(DOCTOR_ID, slotDateTime)).thenReturn(5L);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> appointmentSlotService.validateSlotAvailability(DOCTOR_ID, slotDateTime)
        );

        assertTrue(ex.getMessage().contains("08:00"));
        assertTrue(ex.getMessage().contains("12/06/2026"));
    }

    @Test
    void validateFollowUpSlotAvailability_shouldAllowWhenRemainingSlotExists() {
        LocalDate date = LocalDate.now().plusDays(3);
        LocalDateTime slotDateTime = date.atTime(9, 0);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(DOCTOR_ID, slotDateTime)).thenReturn(2L);

        appointmentSlotService.validateFollowUpSlotAvailability(DOCTOR_ID, slotDateTime);
    }

    @Test
    void hasRemainingSlot_shouldShareCapacityBetweenExamTypes() {
        LocalDate date = LocalDate.of(2026, 6, 12);
        LocalDateTime slotDateTime = date.atTime(9, 0);
        when(appointmentRepository.countBookedSlotsByDoctorAndSlotDateTime(DOCTOR_ID, slotDateTime)).thenReturn(5L);

        assertFalse(appointmentSlotService.hasRemainingSlot(DOCTOR_ID, slotDateTime));
    }


    private LocalDateTime anySlot(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute);
    }

    private AppointmentSlotResponse findSlot(List<AppointmentSlotResponse> slots, String time) {
        return slots.stream()
                .filter(slot -> time.equals(slot.getTime()))
                .findFirst()
                .orElseThrow();
    }

    private AppointmentSlotResponse buildSlot(String time, int totalSlots, long bookedSlots, long remainingSlots, boolean available) {
        LocalDate date = LocalDate.of(2026, 6, 12);
        LocalDateTime start = date.atTime(java.time.LocalTime.parse(time));
        LocalDateTime end = start.plusMinutes(time.endsWith(":30") ? 30 : 60);
        String shift = start.getHour() < 12 ? "morning" : "afternoon";
        return new AppointmentSlotResponse(
                time,
                start,
                end,
                shift,
                totalSlots,
                bookedSlots,
                remainingSlots,
                available,
                available ? null : "FULL"
        );
    }
}
