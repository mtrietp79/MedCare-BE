package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.BookingRulesDto;
import com.medcare.clinic_backend.dto.SlotAvailabilityDto;
import com.medcare.clinic_backend.dto.patient.PatientAppointmentResponse;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.DoctorSchedule;
import com.medcare.clinic_backend.entity.MedicalService;
import com.medcare.clinic_backend.entity.ServicePackage;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING_PAYMENT", "PENDING", "CONFIRMED", "COMPLETED", "CANCELLED");
    private static final int MIN_BOOKING_LEAD_HOURS = 2;
    private static final int MAX_BOOKING_AHEAD_DAYS = 14;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientService patientService;

    @Autowired
    private MedicalServiceService medicalServiceService;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private AppointmentSlotService appointmentSlotService;

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public List<Appointment> getAppointmentsForPatient(Integer patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(patientId);
    }

    public List<PatientAppointmentResponse> getAppointmentResponsesForPatient(Integer patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(patientId)
                .stream()
                .map(this::toPatientAppointmentResponse)
                .toList();
    }

    public List<Appointment> getAppointmentsForDoctor(Integer doctorId) {
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctorId);
    }

    public BookingRulesDto getBookingRules() {
        LocalDateTime serverNow = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime minBookableAt = serverNow.plusHours(MIN_BOOKING_LEAD_HOURS);
        LocalDate maxBookableDate = serverNow.toLocalDate().plusDays(MAX_BOOKING_AHEAD_DAYS);
        return new BookingRulesDto(serverNow, minBookableAt, maxBookableDate, MAX_BOOKING_AHEAD_DAYS);
    }

    public Appointment getAppointmentById(Integer id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich hen ID: " + id));
    }

    public Appointment getAppointmentByIdForPatient(Integer id, Integer patientId) {
        return appointmentRepository.findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich hen ID: " + id));
    }

    public Appointment getAppointmentByIdForDoctor(Integer id, Integer doctorId) {
        return appointmentRepository.findByIdAndDoctorId(id, doctorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich hen ID: " + id));
    }

    @Transactional
    public Appointment createAppointment(Appointment app) {
        if (app == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu lich hen khong hop le.");
        }

        Integer patientId = app.getPatient() == null ? null : app.getPatient().getId();
        patientService.ensureProfileCompleted(patientId);
        applyRequestedMedicalService(app);

        SlotRule slotRule = resolveSlotRule(app.getAppointmentDate());
        app.setAppointmentDate(slotRule.start());
        app.setStatus("PENDING_PAYMENT");
        if (isFollowUpType(app.getAppointmentType())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Lich tai kham chi duoc tao boi bac si sau khi hoan tat buoi kham truoc."
            );
        }
        if (app.getAppointmentType() == null || app.getAppointmentType().isBlank()) {
            app.setAppointmentType("Kh\u00e1m b\u1ec7nh");
        } else {
            app.setAppointmentType(isFollowUpType(app.getAppointmentType()) ? "T\u00e1i kh\u00e1m" : "Kh\u00e1m b\u1ec7nh");
        }
        app.setPaymentStatus("UNPAID");
        app.setAppointmentCode(generateAppointmentCode());

        validateBookingTimeRule(slotRule.start());
        validatePatientAvailability(patientId, slotRule, null);

        if (app.getDoctor() != null && app.getDoctor().getId() != null) {
            Doctor doctor = fetchDoctorForUpdate(app.getDoctor().getId());
            ensureDoctorActiveForBooking(doctor);
            ensureSpecialtyForDoctor(app, doctor);
            appointmentSlotService.validateSlotAvailability(app.getDoctor().getId(), app.getAppointmentDate());
            applyDoctorPricing(app, doctor);
            return appointmentRepository.save(app);
        }

        if (app.getSpecialty() == null || app.getSpecialty().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui long cung cap it nhat chuyen khoa hoac bac si de dat lich.");
        }

        Doctor selectedDoctor = findAvailableDoctorForSpecialty(app.getSpecialty().getId(), appointmentSlotService.buildDailySlotRules(app.getAppointmentDate().toLocalDate()).stream().filter(r -> r.start().equals(app.getAppointmentDate())).findFirst().orElseThrow());
        if (selectedDoctor == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "Hien tai tat ca bac si thuoc khoa nay da kin lich trong khung gio ban chon.");
        }

        app.setSpecialty(selectedDoctor.getSpecialty());
        applyDoctorPricing(app, selectedDoctor);
        return appointmentRepository.save(app);
    }

    public List<SlotAvailabilityDto> getDoctorSlotStatus(Integer doctorId, LocalDate date) {
        if (date == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu ngay can kiem tra slot.");
        }

        Doctor doctor = fetchDoctor(doctorId);
        ensureDoctorActiveForBooking(doctor);

        return appointmentSlotService.getDoctorSlots(doctorId, date);
    }

    public List<SlotAvailabilityDto> getMedicalServiceSlotStatus(Integer serviceId, LocalDate date) {
        if (date == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu ngay can kiem tra slot.");
        }

        MedicalService service = medicalServiceService.getActiveByIdForBooking(serviceId);
        if (service.getSpecialty() == null || service.getSpecialty().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu chua duoc gan chuyen khoa.");
        }
        Doctor assignedDoctor = service.getAssignedDoctor();
        List<Integer> candidateDoctorIds = assignedDoctor != null && assignedDoctor.getId() != null
                ? (Boolean.TRUE.equals(assignedDoctor.getIsActive()) ? List.of(assignedDoctor.getId()) : List.of())
                : doctorRepository.findBySpecialty_IdAndIsActiveTrue(service.getSpecialty().getId()).stream()
                .map(Doctor::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        List<SlotAvailabilityDto> result = new ArrayList<>();
        LocalDateTime serverNow = LocalDateTime.now();
        LocalDateTime minAllowedStart = serverNow.plusHours(MIN_BOOKING_LEAD_HOURS);
        LocalDate maxBookableDate = serverNow.toLocalDate().plusDays(MAX_BOOKING_AHEAD_DAYS);

        for (AppointmentSlotService.SlotRule slotRule : appointmentSlotService.buildDailySlotRules(date)) {
            long totalBookedPatients = candidateDoctorIds.stream()
                    .mapToLong(doctorId -> appointmentRepository.countByDoctorInSlot(doctorId, slotRule.start(), slotRule.end()))
                    .sum();
            int totalMaxPatients = slotRule.maxPatients() * candidateDoctorIds.size();
            boolean hasAvailableDoctor = candidateDoctorIds.stream()
                    .anyMatch(doctorId -> appointmentRepository.countByDoctorInSlot(doctorId, slotRule.start(), slotRule.end()) < slotRule.maxPatients());
            boolean full = candidateDoctorIds.isEmpty() || !hasAvailableDoctor;
            String disabledReason = resolveDisabledReason(slotRule.start(), full, serverNow, minAllowedStart, maxBookableDate);
            boolean disabled = disabledReason != null;

            result.add(new SlotAvailabilityDto(
                    slotRule.start(),
                    slotRule.end(),
                    slotRule.shift().toLowerCase(Locale.ROOT),
                    totalMaxPatients,
                    totalBookedPatients,
                    full,
                    disabled,
                    disabledReason
            ));
        }

        return result;
    }

    @Transactional
    public Appointment updateAppointment(Integer id, Appointment appointmentDetails) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich hen ID: " + id));

        if (appointmentDetails == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu cap nhat khong hop le.");
        }

        Integer targetDoctorId = extractDoctorId(appointmentDetails.getDoctor(), appointment.getDoctor());
        if (targetDoctorId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen phai co bac si.");
        }

        boolean doctorChanged = isDoctorChanged(appointment, appointmentDetails.getDoctor());
        boolean dateChanged = isDateChanged(appointment, appointmentDetails.getAppointmentDate());
        boolean mustRevalidateSlot = doctorChanged || dateChanged;

        Doctor targetDoctor;
        if (mustRevalidateSlot) {
            LocalDateTime targetDate = appointmentDetails.getAppointmentDate() != null
                    ? appointmentDetails.getAppointmentDate()
                    : appointment.getAppointmentDate();

            AppointmentSlotService.SlotRule slotRule = appointmentSlotService.buildDailySlotRules(targetDate.toLocalDate()).stream()
                    .filter(r -> r.start().equals(targetDate.withSecond(0).withNano(0)))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Khung gio khong hop le."));

            validateBookingTimeRule(slotRule.start());
            validatePatientAvailability(appointment.getPatient() == null ? null : appointment.getPatient().getId(), slotRule, appointment.getId());
            targetDoctor = fetchDoctorForUpdate(targetDoctorId);
            applyUpdatedSpecialty(appointment, appointmentDetails, targetDoctor);
            appointmentSlotService.validateSlotAvailability(targetDoctor.getId(), slotRule.start());
            appointment.setAppointmentDate(slotRule.start());
        } else {
            targetDoctor = fetchDoctor(targetDoctorId);
            applyUpdatedSpecialty(appointment, appointmentDetails, targetDoctor);
        }

        appointment.setDoctor(targetDoctor);
        applyUpdatedMedicalService(appointment, appointmentDetails);

        if (appointmentDetails.getStatus() != null && !appointmentDetails.getStatus().isBlank()) {
            validateStatus(appointmentDetails.getStatus());
            appointment.setStatus(appointmentDetails.getStatus().trim().toUpperCase());
        }

        if (appointmentDetails.getSymptoms() != null) {
            appointment.setSymptoms(appointmentDetails.getSymptoms());
        }

        if (appointmentDetails.getAppointmentType() != null && !appointmentDetails.getAppointmentType().isBlank()) {
            appointment.setAppointmentType(
                    isFollowUpType(appointmentDetails.getAppointmentType()) ? "T\u00e1i kh\u00e1m" : "Kh\u00e1m b\u1ec7nh"
            );
        }

        applyAppointmentPricing(appointment, targetDoctor);

        if (appointmentDetails.getNotes() != null) {
            appointment.setNotes(appointmentDetails.getNotes());
        }

        if (appointmentDetails.getPaymentStatus() != null && !appointmentDetails.getPaymentStatus().isBlank()) {
            appointment.setPaymentStatus(appointmentDetails.getPaymentStatus());
        }

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment cancelAppointmentByPatient(Integer appointmentId, Integer patientId) {
        if (appointmentId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu appointmentId.");
        }
        if (patientId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong xac dinh duoc benh nhan.");
        }

        Appointment appointment = appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich hen ID: " + appointmentId));

        String currentStatus = appointment.getStatus() == null ? "" : appointment.getStatus().trim().toUpperCase();
        if ("COMPLETED".equals(currentStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong the huy lich hen da hoan tat.");
        }
        if ("CANCELLED".equals(currentStatus)) {
            return appointment;
        }

        appointment.setStatus("CANCELLED");
        if (!"PAID".equalsIgnoreCase(appointment.getPaymentStatus())
                && !"PAID_ONLINE".equalsIgnoreCase(appointment.getPaymentStatus())) {
            appointment.setPaymentStatus("CANCELLED");
        }
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public void deleteAppointment(Integer id) {
        if (!appointmentRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich hen ID: " + id);
        }
        appointmentRepository.deleteById(id);
    }

    private Doctor findAvailableDoctorForSpecialty(Integer specialtyId, AppointmentSlotService.SlotRule slotRule) {
        List<Integer> candidateDoctorIds = doctorRepository.findBySpecialty_IdAndIsActiveTrue(specialtyId).stream()
                .map(Doctor::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (candidateDoctorIds.isEmpty()) {
            return null;
        }

        List<Integer> orderedDoctorIds = new ArrayList<>(candidateDoctorIds);
        Collections.shuffle(orderedDoctorIds);

        for (Integer doctorId : orderedDoctorIds) {
            Doctor lockedDoctor = fetchDoctorForUpdate(doctorId);
            long currentCount = appointmentRepository.countByDoctorInSlot(lockedDoctor.getId(), slotRule.start(), slotRule.end());
            if (currentCount < slotRule.maxPatients()) {
                return lockedDoctor;
            }
        }

        return null;
    }

    private Integer extractDoctorId(Doctor fromRequest, Doctor fromExisting) {
        if (fromRequest != null && fromRequest.getId() != null) {
            return fromRequest.getId();
        }
        if (fromExisting != null && fromExisting.getId() != null) {
            return fromExisting.getId();
        }
        return null;
    }

    private boolean isDoctorChanged(Appointment existing, Doctor requestedDoctor) {
        if (requestedDoctor == null || requestedDoctor.getId() == null) {
            return false;
        }
        Integer existingDoctorId = existing.getDoctor() == null ? null : existing.getDoctor().getId();
        return !requestedDoctor.getId().equals(existingDoctorId);
    }

    private boolean isDateChanged(Appointment existing, LocalDateTime requestedDate) {
        if (requestedDate == null) {
            return false;
        }
        LocalDateTime normalizedRequested = requestedDate.withSecond(0).withNano(0);
        LocalDateTime normalizedExisting = existing.getAppointmentDate() == null
                ? null
                : existing.getAppointmentDate().withSecond(0).withNano(0);
        return !normalizedRequested.equals(normalizedExisting);
    }

    private Doctor fetchDoctor(Integer doctorId) {
        if (doctorId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu doctorId.");
        }
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay bac si ID: " + doctorId));
    }

    private Doctor fetchDoctorForUpdate(Integer doctorId) {
        if (doctorId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu doctorId.");
        }
        return doctorRepository.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay bac si ID: " + doctorId));
    }

    private void ensureDoctorActiveForBooking(Doctor doctor) {
        if (doctor == null) {
            return;
        }
        if (!Boolean.TRUE.equals(doctor.getIsActive())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si dang tam ngung hoat dong.");
        }
    }

    private void validateDoctorAvailability(Doctor doctor, AppointmentSlotService.SlotRule slotRule, Integer excludedAppointmentId) {
        long count = excludedAppointmentId == null
                ? appointmentRepository.countByDoctorInSlot(doctor.getId(), slotRule.start(), slotRule.end())
                : appointmentRepository.countByDoctorInSlotExcludingAppointment(doctor.getId(), slotRule.start(), slotRule.end(), excludedAppointmentId);

        if (count >= slotRule.maxPatients()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Khung gio nay cua bac si da day. Vui long chon gio khac.");
        }
    }

    private void validatePatientAvailability(Integer patientId, AppointmentSlotService.SlotRule slotRule, Integer excludedAppointmentId) {
        if (patientId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong xac dinh duoc benh nhan dat lich.");
        }

        long count = excludedAppointmentId == null
                ? appointmentRepository.countByPatientInSlot(patientId, slotRule.start(), slotRule.end())
                : appointmentRepository.countByPatientInSlotExcludingAppointment(
                patientId,
                slotRule.start(),
                slotRule.end(),
                excludedAppointmentId
        );

        if (count > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Ban da co lich hen trong khung gio nay.");
        }
    }

    private void validateBookingTimeRule(LocalDateTime slotStart) {
        LocalDateTime now = LocalDateTime.now();
        if (slotStart.isBefore(now)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong the dat lich o thoi diem da qua.");
        }
        LocalDate maxBookableDate = now.toLocalDate().plusDays(MAX_BOOKING_AHEAD_DAYS);
        if (slotStart.toLocalDate().isAfter(maxBookableDate)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Chi duoc dat lich toi da " + MAX_BOOKING_AHEAD_DAYS
                            + " ngay ke tu hien tai (den ngay " + maxBookableDate.format(DATE_FORMATTER) + ")."
            );
        }

        LocalDateTime minAllowedStart = now.plusHours(MIN_BOOKING_LEAD_HOURS);
        if (slotStart.isBefore(minAllowedStart)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Lich hen phai dat truoc it nhat " + MIN_BOOKING_LEAD_HOURS + " tieng theo thoi gian hien tai."
            );
        }
    }

    private String resolveDisabledReason(
            LocalDateTime slotStart,
            boolean full,
            LocalDateTime serverNow,
            LocalDateTime minAllowedStart,
            LocalDate maxBookableDate
    ) {
        if (slotStart.isBefore(serverNow)) {
            return "PAST";
        }
        if (slotStart.toLocalDate().isAfter(maxBookableDate)) {
            return "TOO_FAR";
        }
        if (slotStart.isBefore(minAllowedStart)) {
            return "LESS_THAN_2H";
        }
        if (full) {
            return "FULL";
        }
        return null;
    }

    private String resolveDoctorScheduleDisabledReason(
            boolean hasConfiguredSchedule,
            Set<String> availableShifts,
            String slotShift
    ) {
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
            return "";
        }
        return shift.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private void ensureSpecialtyForDoctor(Appointment appointment, Doctor doctor) {
        Specialty doctorSpecialty = doctor.getSpecialty();
        if (doctorSpecialty == null || doctorSpecialty.getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si chua duoc gan chuyen khoa.");
        }

        if (appointment.getSpecialty() == null || appointment.getSpecialty().getId() == null) {
            appointment.setSpecialty(doctorSpecialty);
            return;
        }

        if (!appointment.getSpecialty().getId().equals(doctorSpecialty.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si khong thuoc chuyen khoa da chon.");
        }

        appointment.setSpecialty(doctorSpecialty);
    }

    private void applyRequestedMedicalService(Appointment appointment) {
        if (appointment.getMedicalService() == null || appointment.getMedicalService().getId() == null) {
            appointment.setMedicalService(null);
            return;
        }

        MedicalService selectedService = medicalServiceService.getActiveByIdForBooking(appointment.getMedicalService().getId());
        Specialty serviceSpecialty = selectedService.getSpecialty();
        if (serviceSpecialty == null || serviceSpecialty.getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu chua duoc gan chuyen khoa.");
        }

        if (appointment.getSpecialty() == null || appointment.getSpecialty().getId() == null) {
            appointment.setSpecialty(serviceSpecialty);
        } else if (!appointment.getSpecialty().getId().equals(serviceSpecialty.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu khong thuoc chuyen khoa da chon.");
        }

        Doctor assignedDoctor = selectedService.getAssignedDoctor();
        if (assignedDoctor != null && assignedDoctor.getId() != null) {
            if (appointment.getDoctor() != null
                    && appointment.getDoctor().getId() != null
                    && !appointment.getDoctor().getId().equals(assignedDoctor.getId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu nay da co bac si dam nhan rieng.");
            }
            appointment.setDoctor(assignedDoctor);
        }

        appointment.setMedicalService(selectedService);
    }

    private void applyUpdatedMedicalService(Appointment appointment, Appointment appointmentDetails) {
        if (appointmentDetails.getMedicalService() == null) {
            validateExistingMedicalServiceSpecialty(appointment);
            return;
        }

        if (appointmentDetails.getMedicalService().getId() == null) {
            appointment.setMedicalService(null);
            return;
        }

        MedicalService selectedService = medicalServiceService.getActiveByIdForBooking(appointmentDetails.getMedicalService().getId());
        Specialty serviceSpecialty = selectedService.getSpecialty();
        Integer appointmentSpecialtyId = appointment.getSpecialty() == null ? null : appointment.getSpecialty().getId();

        if (serviceSpecialty == null || serviceSpecialty.getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu chua duoc gan chuyen khoa.");
        }
        if (appointmentSpecialtyId != null && !appointmentSpecialtyId.equals(serviceSpecialty.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu khong thuoc chuyen khoa cua lich hen.");
        }

        Doctor assignedDoctor = selectedService.getAssignedDoctor();
        Integer appointmentDoctorId = appointment.getDoctor() == null ? null : appointment.getDoctor().getId();
        if (assignedDoctor != null
                && assignedDoctor.getId() != null
                && appointmentDoctorId != null
                && !assignedDoctor.getId().equals(appointmentDoctorId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu nay da co bac si dam nhan rieng.");
        }

        appointment.setMedicalService(selectedService);
    }

    private void validateExistingMedicalServiceSpecialty(Appointment appointment) {
        if (appointment.getMedicalService() == null || appointment.getMedicalService().getId() == null) {
            return;
        }

        MedicalService selectedService = medicalServiceService.getActiveByIdForBooking(appointment.getMedicalService().getId());
        Specialty serviceSpecialty = selectedService.getSpecialty();
        Integer appointmentSpecialtyId = appointment.getSpecialty() == null ? null : appointment.getSpecialty().getId();
        if (serviceSpecialty != null && serviceSpecialty.getId() != null
                && appointmentSpecialtyId != null
                && !appointmentSpecialtyId.equals(serviceSpecialty.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu khong thuoc chuyen khoa cua lich hen.");
        }
        appointment.setMedicalService(selectedService);
    }

    private void applyUpdatedSpecialty(Appointment appointment, Appointment appointmentDetails, Doctor doctor) {
        Specialty requestSpecialty = appointmentDetails.getSpecialty();
        Specialty doctorSpecialty = doctor.getSpecialty();

        if (doctorSpecialty == null || doctorSpecialty.getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si chua duoc gan chuyen khoa.");
        }

        if (requestSpecialty != null && requestSpecialty.getId() != null) {
            if (!requestSpecialty.getId().equals(doctorSpecialty.getId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si khong thuoc chuyen khoa da chon.");
            }
            appointment.setSpecialty(doctorSpecialty);
            return;
        }

        if (appointment.getSpecialty() != null && appointment.getSpecialty().getId() != null) {
            if (!appointment.getSpecialty().getId().equals(doctorSpecialty.getId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si khong thuoc chuyen khoa hien tai cua lich hen.");
            }
            appointment.setSpecialty(doctorSpecialty);
            return;
        }

        appointment.setSpecialty(doctorSpecialty);
    }

    private void applyDoctorPricing(Appointment appointment, Doctor doctor) {
        appointment.setDoctor(doctor);
        applyAppointmentPricing(appointment, doctor);
    }

    private void applyAppointmentPricing(Appointment appointment, Doctor doctor) {
        if (isFollowUpType(appointment.getAppointmentType())) {
            appointment.setConsultationFee(resolveConsultationFee(doctor) * 0.5);
            return;
        }

        ServicePackage servicePackage = appointment.getServicePackage();
        if (servicePackage != null && servicePackage.getId() != null) {
            if (servicePackage.getPrice() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Goi dich vu chua co gia hop le.");
            }
            appointment.setConsultationFee(servicePackage.getPrice());
            return;
        }

        if (appointment.getMedicalService() != null && appointment.getMedicalService().getId() != null) {
            MedicalService selectedService = medicalServiceService.getActiveByIdForBooking(appointment.getMedicalService().getId());
            appointment.setMedicalService(selectedService);
            appointment.setConsultationFee(selectedService.getPrice());
            return;
        }

        appointment.setConsultationFee(resolveConsultationFee(doctor));
    }

    private Double resolveConsultationFee(Doctor doctor) {
        if (doctor == null || doctor.getId() == null) {
            return null;
        }

        BigDecimal price = doctor.getPrice();
        if (price == null) {
            Doctor persistedDoctor = fetchDoctor(doctor.getId());
            price = persistedDoctor.getPrice();
        }

        if (price == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si chua co gia kham. Vui long cap nhat gia cho bac si ID: " + doctor.getId());
        }

        return price.doubleValue();
    }

    private boolean isFollowUpType(String type) {
        if (type == null || type.isBlank()) {
            return false;
        }
        String folded = Normalizer.normalize(type, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase(java.util.Locale.ROOT)
                .replace(" ", "");
        return folded.contains("taikham");
    }

    private void validateStatus(String status) {
        String normalizedStatus = status.trim().toUpperCase();
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Trang thai lich hen khong hop le.");
        }
    }

    private AppointmentSlotService.SlotRule resolveSlotRule(LocalDateTime appointmentDate) {
        if (appointmentDate == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu thoi gian dat lich.");
        }

        LocalDateTime normalizedDateTime = appointmentDate.withSecond(0).withNano(0);
        for (AppointmentSlotService.SlotRule slotRule : appointmentSlotService.buildDailySlotRules(normalizedDateTime.toLocalDate())) {
            if (slotRule.start().equals(normalizedDateTime)) {
                return slotRule;
            }
        }

        throw new BusinessException(HttpStatus.BAD_REQUEST,
                "Gio dat lich khong hop le. Chi ho tro 07:30, 08:00, 09:00, 10:00, 12:30, 13:00, 14:00, 15:00.");
    }

    private String generateAppointmentCode() {
        String code;
        do {
            code = "PKB-" + System.currentTimeMillis();
        } while (appointmentRepository.existsByAppointmentCode(code));
        return code;
    }

    private PatientAppointmentResponse toPatientAppointmentResponse(Appointment appointment) {
        LocalDateTime dateTime = appointment == null ? null : appointment.getAppointmentDate();
        LocalTime appointmentTime = dateTime == null ? null : dateTime.toLocalTime();
        String appointmentType = resolveAppointmentType(appointment);
        return new PatientAppointmentResponse(
                appointment == null ? null : appointment.getId(),
                appointment == null ? null : appointment.getAppointmentCode(),
                appointment == null ? null : appointment.getDoctorName(),
                resolveSpecialtyName(appointment),
                dateTime == null ? null : dateTime.toLocalDate(),
                appointmentTime,
                appointmentTime == null ? null : appointmentTime.format(TIME_FORMATTER),
                appointmentType,
                resolveStatusDisplay(appointment == null ? null : appointment.getStatus()),
                appointment == null ? null : appointment.getConsultationFee(),
                resolvePaymentStatusDisplay(appointment == null ? null : appointment.getPaymentStatus()),
                appointment == null ? null : appointment.getParentAppointmentId(),
                isFollowUpAppointment(appointment) ? resolveFollowUpNote(appointment) : null
        );
    }

    private String resolveSpecialtyName(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        String specialtyName = appointment.getSpecialtyName();
        if (specialtyName != null && !specialtyName.isBlank()) {
            return specialtyName;
        }
        Doctor doctor = appointment.getDoctor();
        return doctor == null || doctor.getSpecialty() == null ? null : doctor.getSpecialty().getName();
    }

    private boolean isFollowUpAppointment(Appointment appointment) {
        return appointment != null
                && (isFollowUpType(appointment.getAppointmentType())
                || appointment.getParentAppointmentId() != null
                || trimToNull(appointment.getFollowUpNote()) != null);
    }

    private String resolveAppointmentType(Appointment appointment) {
        return isFollowUpAppointment(appointment) ? "T\u00e1i kh\u00e1m" : "Kh\u00e1m b\u1ec7nh";
    }

    private String resolveFollowUpNote(Appointment appointment) {
        String followUpNote = appointment == null ? null : trimToNull(appointment.getFollowUpNote());
        if (followUpNote != null) {
            return followUpNote;
        }
        return appointment == null ? null : trimToNull(appointment.getNotes());
    }

    private String resolveStatusDisplay(String status) {
        String normalized = normalizeTextForCompare(status);
        if (normalized == null) {
            return "Ch\u01b0a kh\u00e1m";
        }
        if (normalized.contains("cancel") || normalized.contains("huy")) {
            return "H\u1ee7y l\u1ecbch";
        }
        if (normalized.contains("completed") || normalized.contains("dakham")) {
            return "\u0110\u00e3 kh\u00e1m";
        }
        return "Ch\u01b0a kh\u00e1m";
    }

    private String resolvePaymentStatusDisplay(String status) {
        String normalized = normalizeTextForCompare(status);
        if (normalized == null || normalized.contains("unpaid") || normalized.contains("chuathanhtoan")) {
            return "Ch\u01b0a thanh to\u00e1n";
        }
        if (normalized.equals("paid") || normalized.contains("paidonline") || normalized.contains("dathanhtoan")) {
            return "\u0110\u00e3 thanh to\u00e1n";
        }
        if (normalized.contains("fail")) {
            return "Thanh to\u00e1n th\u1ea5t b\u1ea1i";
        }
        if (normalized.contains("cancel") || normalized.contains("huy")) {
            return "\u0110\u00e3 h\u1ee7y";
        }
        return "Ch\u01b0a thanh to\u00e1n";
    }

    private String normalizeTextForCompare(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Logic Slot cũ đã được chuyển sang AppointmentSlotService
}

