package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.SlotAvailabilityDto;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.DoctorSchedule;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorScheduleRepository scheduleRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Appointment getAppointmentById(Integer id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay lich hen ID: " + id));
    }

    public Appointment createAppointment(Appointment app) {
        SlotRule slotRule = resolveSlotRule(app.getAppointmentDate());

        if (app.getDoctor() != null && app.getDoctor().getId() != null) {
            Doctor doctor = fetchDoctor(app.getDoctor().getId());
            validateDoctorAvailability(doctor, slotRule);
            applyDoctorPricing(app, doctor);
            return appointmentRepository.save(app);
        }

        if (app.getSpecialty() != null && app.getSpecialty().getId() != null) {
            List<DoctorSchedule> availableSchedules = scheduleRepository.findByWorkDate(slotRule.date());
            Doctor selectedDoctor = null;
            long minPatientCount = Long.MAX_VALUE;

            for (DoctorSchedule schedule : availableSchedules) {
                Doctor doctor = schedule.getDoctor();
                if (doctor == null || doctor.getSpecialty() == null) {
                    continue;
                }

                if (!doctor.getSpecialty().getId().equals(app.getSpecialty().getId())) {
                    continue;
                }

                if (!isScheduleMatchingShift(schedule, slotRule.shift())) {
                    continue;
                }

                long currentCount = appointmentRepository.countByDoctorInSlot(
                        doctor.getId(),
                        slotRule.start(),
                        slotRule.end()
                );

                if (currentCount < slotRule.maxPatients() && currentCount < minPatientCount) {
                    minPatientCount = currentCount;
                    selectedDoctor = doctor;
                }
            }

            if (selectedDoctor == null) {
                throw new RuntimeException("Hien tai tat ca bac si thuoc khoa nay da kin lich trong khung gio ban chon.");
            }

            applyDoctorPricing(app, selectedDoctor);
            return appointmentRepository.save(app);
        }

        throw new RuntimeException("Vui long cung cap it nhat chuyen khoa hoac bac si de dat lich.");
    }

    public List<SlotAvailabilityDto> getDoctorSlotStatus(Integer doctorId, LocalDate date) {
        if (date == null) {
            throw new RuntimeException("Thieu ngay can kiem tra slot.");
        }

        Doctor doctor = fetchDoctor(doctorId);
        List<DoctorSchedule> schedules = scheduleRepository.findByDoctorIdAndWorkDate(doctor.getId(), date);
        List<SlotAvailabilityDto> result = new ArrayList<>();

        for (SlotRule slotRule : buildDailySlotRules(date)) {
            boolean onShift = schedules.stream().anyMatch(schedule -> isScheduleMatchingShift(schedule, slotRule.shift()));
            long bookedPatients = onShift
                    ? appointmentRepository.countByDoctorInSlot(doctor.getId(), slotRule.start(), slotRule.end())
                    : 0L;
            boolean full = onShift && bookedPatients >= slotRule.maxPatients();
            boolean disabled = !onShift || full;

            result.add(new SlotAvailabilityDto(
                    slotRule.start(),
                    slotRule.end(),
                    slotRule.shift(),
                    slotRule.maxPatients(),
                    bookedPatients,
                    full,
                    disabled
            ));
        }

        return result;
    }

    public Appointment updateAppointment(Integer id, Appointment appointmentDetails) {
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment == null) {
            return null;
        }

        appointment.setPatient(appointmentDetails.getPatient());
        appointment.setSpecialty(appointmentDetails.getSpecialty());
        appointment.setDoctor(appointmentDetails.getDoctor());
        appointment.setAppointmentDate(appointmentDetails.getAppointmentDate());
        appointment.setStatus(appointmentDetails.getStatus());
        appointment.setSymptoms(appointmentDetails.getSymptoms());
        appointment.setConsultationFee(resolveConsultationFee(appointmentDetails.getDoctor()));
        return appointmentRepository.save(appointment);
    }

    public void deleteAppointment(Integer id) {
        appointmentRepository.deleteById(id);
    }

    private Doctor fetchDoctor(Integer doctorId) {
        if (doctorId == null) {
            throw new RuntimeException("Thieu doctorId.");
        }
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay bac si ID: " + doctorId));
    }

    private void validateDoctorAvailability(Doctor doctor, SlotRule slotRule) {
        List<DoctorSchedule> schedules = scheduleRepository.findByDoctorIdAndWorkDate(doctor.getId(), slotRule.date());
        boolean availableInShift = schedules.stream().anyMatch(schedule -> isScheduleMatchingShift(schedule, slotRule.shift()));

        if (!availableInShift) {
            throw new RuntimeException("Bac si khong co lich truc trong ca ban chon.");
        }

        long count = appointmentRepository.countByDoctorInSlot(doctor.getId(), slotRule.start(), slotRule.end());
        if (count >= slotRule.maxPatients()) {
            throw new RuntimeException("Khung gio nay cua bac si da day. Vui long chon gio khac.");
        }
    }

    private void applyDoctorPricing(Appointment appointment, Doctor doctor) {
        appointment.setDoctor(doctor);
        appointment.setConsultationFee(resolveConsultationFee(doctor));
    }

    private Double resolveConsultationFee(Doctor doctor) {
        if (doctor == null || doctor.getId() == null) {
            return null;
        }

        Doctor persistedDoctor = fetchDoctor(doctor.getId());
        BigDecimal price = persistedDoctor.getPrice();
        if (price == null) {
            throw new RuntimeException("Bac si chua co gia kham. Vui long cap nhat gia cho bac si ID: " + doctor.getId());
        }

        return price.doubleValue();
    }

    private boolean isScheduleMatchingShift(DoctorSchedule schedule, String requiredShift) {
        if (schedule == null || schedule.getShift() == null) {
            return false;
        }

        String shift = schedule.getShift().trim().toUpperCase();
        String normalizedRequiredShift = requiredShift.trim().toUpperCase();

        if ("ALL_DAY".equals(shift)) {
            return true;
        }

        if ("AFTERNOON".equals(normalizedRequiredShift) && "EVENING".equals(shift)) {
            return true;
        }

        return shift.equals(normalizedRequiredShift);
    }

    private SlotRule resolveSlotRule(LocalDateTime appointmentDate) {
        if (appointmentDate == null) {
            throw new RuntimeException("Thieu thoi gian dat lich.");
        }

        LocalDateTime normalizedDateTime = appointmentDate.withSecond(0).withNano(0);
        for (SlotRule slotRule : buildDailySlotRules(normalizedDateTime.toLocalDate())) {
            if (slotRule.start().equals(normalizedDateTime)) {
                return slotRule;
            }
        }

        throw new RuntimeException("Gio dat lich khong hop le. Chi ho tro 07:30, 08:00, 09:00, 10:00, 12:30, 13:00, 14:00, 15:00.");
    }

    private List<SlotRule> buildDailySlotRules(LocalDate date) {
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

    private record SlotRule(LocalDate date, LocalDateTime start, LocalDateTime end, String shift, int maxPatients) {
    }
}
