package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.SlotAvailabilityDto;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.DoctorSchedule;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientService patientService;

    @Mock
    private MedicalServiceService medicalServiceService;

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private AppointmentSlotService appointmentSlotService;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void getDoctorSlotStatus_shouldDisableAllSlotsWhenDoctorHasNoScheduleOnDate() {
        Doctor doctor = buildDoctor(7);
        LocalDate date = LocalDate.now().plusDays(5);

        when(doctorRepository.findById(7)).thenReturn(Optional.of(doctor));
        when(appointmentSlotService.getDoctorSlotsForPatientBooking(7, date)).thenReturn(List.of(
                new SlotAvailabilityDto(
                        date.atTime(8, 0),
                        date.atTime(9, 0),
                        "morning",
                        5,
                        0,
                        false,
                        true,
                        "NO_SCHEDULE"
                )
        ));

        List<SlotAvailabilityDto> slots = appointmentService.getDoctorSlotStatus(7, date);

        assertTrue(slots.stream().allMatch(slot ->
                slot.disabled() && "NO_SCHEDULE".equals(slot.disabledReason())
        ));
    }

    @Test
    void getDoctorSlotStatus_shouldDisableOnlyUnavailableShift() {
        Doctor doctor = buildDoctor(7);
        LocalDate date = LocalDate.now().plusDays(6);

        when(doctorRepository.findById(7)).thenReturn(Optional.of(doctor));
        when(appointmentSlotService.getDoctorSlotsForPatientBooking(7, date)).thenReturn(List.of(
                new SlotAvailabilityDto(
                        date.atTime(8, 0),
                        date.atTime(9, 0),
                        "morning",
                        5,
                        0,
                        false,
                        false,
                        null
                ),
                new SlotAvailabilityDto(
                        date.atTime(13, 0),
                        date.atTime(14, 0),
                        "afternoon",
                        5,
                        0,
                        false,
                        true,
                        "SHIFT_UNAVAILABLE"
                )
        ));

        List<SlotAvailabilityDto> slots = appointmentService.getDoctorSlotStatus(7, date);

        assertTrue(slots.stream().anyMatch(slot ->
                "morning".equals(slot.shift()) && !slot.disabled()
        ));
        assertTrue(slots.stream().anyMatch(slot ->
                "afternoon".equals(slot.shift())
                        && slot.disabled()
                        && "SHIFT_UNAVAILABLE".equals(slot.disabledReason())
        ));
    }

    private Doctor buildDoctor(int id) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setIsActive(true);
        return doctor;
    }

    private DoctorSchedule buildSchedule(Doctor doctor, LocalDate date, String shift) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        schedule.setWorkDate(date);
        schedule.setShift(shift);
        return schedule;
    }
}
