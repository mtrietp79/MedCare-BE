package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.DoctorResponse;
import com.medcare.clinic_backend.dto.doctor.*;
import com.medcare.clinic_backend.entity.*;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorPortalService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String DISPLAY_STATUS_PENDING = "Chờ khám";
    private static final String DISPLAY_STATUS_COMPLETED = "Đã khám";
    private static final String DISPLAY_STATUS_CANCELLED = "Hủy lịch";

    private static final String TYPE_NEW_EXAM = "Khám bệnh";
    private static final String TYPE_FOLLOW_UP = "Tái khám";

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorPhotoRepository doctorPhotoRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private PrescriptionDetailRepository prescriptionDetailRepository;

    @Autowired
    private ServiceDetailRepository serviceDetailRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private MedicalServiceRepository medicalServiceRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private MedicineService medicineService;

    private static final DateTimeFormatter TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public DoctorDashboardResponse getDashboard(String username) {
        Doctor doctor = getDoctorByUsername(username);
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        List<Appointment> todayAppointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAsc(
                        doctor.getId(),
                        startOfDay,
                        endOfDay
                );

        long pendingAppointments = todayAppointments.stream()
                .filter(this::isPendingForDoctorFlow)
                .count();

        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);
        List<Appointment> monthAppointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateDesc(
                        doctor.getId(),
                        startOfMonth,
                        endOfMonth
                );
        long completedThisMonth = monthAppointments.stream()
                .filter(this::isCompletedForDoctorFlow)
                .count();

        double satisfactionRate = resolveSatisfactionRate(doctor);
        return new DoctorDashboardResponse(
                todayAppointments.size(),
                pendingAppointments,
                completedThisMonth,
                satisfactionRate
        );
    }

    public List<DoctorAppointmentListItemResponse> getAppointments(
            String username,
            String keyword,
            String status,
            LocalDate date,
            String type
    ) {
        Doctor doctor = getDoctorByUsername(username);
        List<Appointment> appointments = appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctor.getId());

        String normalizedKeyword = normalizeText(keyword);
        AppointmentStatusFilter statusFilter = parseStatusFilter(status);
        AppointmentTypeFilter typeFilter = parseTypeFilter(type);

        return appointments.stream()
                .filter(appointment -> matchesKeyword(appointment, normalizedKeyword))
                .filter(appointment -> matchesStatusFilter(appointment, statusFilter))
                .filter(appointment -> matchesDateFilter(appointment, date))
                .filter(appointment -> matchesTypeFilter(appointment, typeFilter))
                .map(this::toAppointmentListItem)
                .collect(Collectors.toList());
    }

    public DoctorAppointmentDetailResponse getAppointmentDetail(String username, Integer appointmentId) {
        Doctor doctor = getDoctorByUsername(username);
        Appointment appointment = appointmentRepository.findByIdAndDoctorId(appointmentId, doctor.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich hen."));

        Patient patient = appointment.getPatient();
        Doctor appointmentDoctor = appointment.getDoctor();
        Specialty specialty = appointment.getSpecialty();
        LocalDateTime appointmentDateTime = appointment.getAppointmentDate();

        return new DoctorAppointmentDetailResponse(
                appointment.getId(),
                new DoctorAppointmentDetailResponse.PatientInfo(
                        patient == null ? null : patient.getId(),
                        patient == null ? null : patient.getFullName(),
                        patient == null ? null : patient.getPhone(),
                        patient == null ? null : patient.getEmail(),
                        patient == null ? null : patient.getGender(),
                        patient == null ? null : patient.getDateOfBirth(),
                        patient == null ? null : patient.getAddress()
                ),
                new DoctorAppointmentDetailResponse.DoctorInfo(
                        appointmentDoctor == null ? null : appointmentDoctor.getId(),
                        appointmentDoctor == null ? null : appointmentDoctor.getFullName(),
                        appointmentDoctor == null ? null : appointmentDoctor.getEmail(),
                        appointmentDoctor == null ? null : appointmentDoctor.getPhone()
                ),
                new DoctorAppointmentDetailResponse.SpecialtyInfo(
                        specialty == null ? null : specialty.getId(),
                        specialty == null ? null : specialty.getName()
                ),
                appointmentDateTime == null ? null : appointmentDateTime.toLocalDate(),
                appointmentDateTime == null ? null : appointmentDateTime.toLocalTime(),
                formatTimeLabel(appointmentDateTime == null ? null : appointmentDateTime.toLocalTime()),
                resolveDisplayType(appointment.getType()),
                resolveDisplayStatus(appointment.getStatus()),
                appointment.getNotes(),
                appointment.getSymptoms()
        );
    }

    @Transactional
    public CompleteAppointmentResponse completeAppointment(
            String username,
            Integer appointmentId,
            CompleteAppointmentRequest request
    ) {
        Doctor doctor = getDoctorByUsername(username);
        Appointment appointment = appointmentRepository.findByIdAndDoctorId(appointmentId, doctor.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich hen."));

        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu kham benh khong hop le.");
        }
        if (request.getDiagnosis() == null || request.getDiagnosis().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Chan doan khong duoc de trong.");
        }
        if (isCancelledForDoctorFlow(appointment)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Không thể khám lịch hẹn đã bị hủy.");
        }
        if (isCompletedForDoctorFlow(appointment) || medicalRecordRepository.existsByAppointmentId(appointment.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen da duoc hoan tat, khong the complete lai.");
        }

        appointment.setType(resolveDisplayType(appointment.getType()));
        appointment.setSymptoms(trimToNull(request.getSymptoms()));
        appointment.setStatus(STATUS_COMPLETED);
        appointmentRepository.save(appointment);

        MedicalRecord record = new MedicalRecord();
        record.setAppointment(appointment);
        record.setDoctor(doctor);
        record.setPatient(appointment.getPatient());
        record.setExaminationDate(
                appointment.getAppointmentDate() == null
                        ? LocalDate.now()
                        : appointment.getAppointmentDate().toLocalDate()
        );
        record.setDiagnosis(request.getDiagnosis().trim());
        record.setDoctorAdvice(trimToNull(request.getDoctorAdvice()));
        record.setTreatmentPlan(null);
        record.setPrescription(null);
        MedicalRecord savedRecord = medicalRecordRepository.save(record);

        createPrescriptionItems(savedRecord, request.getMedicineItems());
        createServiceItems(savedRecord, request.getServiceItems());
        invoiceService.createInvoiceFromRecord(savedRecord);

        Integer followUpAppointmentId = null;
        if (request.getFollowUp() != null && Boolean.TRUE.equals(request.getFollowUp().getNeedFollowUp())) {
            Appointment followUpAppointment = createFollowUpAppointment(
                    appointment,
                    request.getFollowUp().getFollowUpDate(),
                    request.getFollowUp().getFollowUpTime(),
                    request.getFollowUp().getNote()
            );
            followUpAppointmentId = followUpAppointment.getId();
        }

        return new CompleteAppointmentResponse(
                appointment.getId(),
                savedRecord.getId(),
                resolveDisplayStatus(appointment.getStatus()),
                followUpAppointmentId
        );
    }

    public List<DoctorMedicineResponse> getMedicinesForDoctor(String username) {
        getDoctorByUsername(username);
        return medicineRepository.findAll().stream()
                .map(medicine -> {
                    String status = medicineService.resolveStockStatus(medicine.getUnit(), medicine.getQuantity());
                    return new DoctorMedicineResponse(
                            medicine.getId(),
                            medicine.getName(),
                            medicine.getUnit(),
                            medicine.getQuantity(),
                            medicine.getDosage(),
                            medicine.getPrice(),
                            status
                    );
                })
                .filter(item -> "Còn hàng".equals(item.getStatus()) || "Sắp hết".equals(item.getStatus()))
                .collect(Collectors.toList());
    }

    public List<DoctorMedicalServiceResponse> getMedicalServicesForDoctor(String username) {
        getDoctorByUsername(username);
        try {
            return medicalServiceRepository.findByActiveTrueOrderByIdDesc().stream()
                    .map(service -> new DoctorMedicalServiceResponse(
                            service.getId(),
                            service.getName(),
                            service.getPrice(),
                            service.getDescription()
                    ))
                    .collect(Collectors.toList());
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    public DoctorMedicalRecordsSummaryResponse getMedicalRecordSummary(String username) {
        Doctor doctor = getDoctorByUsername(username);
        List<MedicalRecord> records = medicalRecordRepository.findByDoctorIdOrderByExaminationDateDesc(doctor.getId());

        Map<Integer, Long> visitsByPatient = records.stream()
                .filter(record -> record.getPatient() != null && record.getPatient().getId() != null)
                .collect(Collectors.groupingBy(record -> record.getPatient().getId(), Collectors.counting()));

        long totalPatients = visitsByPatient.size();
        long newPatients = visitsByPatient.values().stream().filter(count -> count == 1).count();
        long followUpPatients = appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctor.getId()).stream()
                .filter(appointment -> TYPE_FOLLOW_UP.equals(resolveDisplayType(appointment.getType())))
                .map(appointment -> appointment.getPatient() == null ? null : appointment.getPatient().getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new DoctorMedicalRecordsSummaryResponse(totalPatients, newPatients, followUpPatients);
    }

    public List<DoctorMedicalRecordPatientItemResponse> getMedicalRecordPatients(String username, String keyword) {
        Doctor doctor = getDoctorByUsername(username);
        List<MedicalRecord> records = medicalRecordRepository.findByDoctorIdOrderByExaminationDateDesc(doctor.getId());
        String normalizedKeyword = normalizeText(keyword);

        Map<Integer, List<MedicalRecord>> recordsByPatient = records.stream()
                .filter(record -> record.getPatient() != null && record.getPatient().getId() != null)
                .collect(Collectors.groupingBy(record -> record.getPatient().getId()));

        List<DoctorMedicalRecordPatientItemResponse> result = new ArrayList<>();
        for (Map.Entry<Integer, List<MedicalRecord>> entry : recordsByPatient.entrySet()) {
            Patient patient = entry.getValue().get(0).getPatient();
            if (!matchesPatientKeyword(patient, normalizedKeyword)) {
                continue;
            }

            LocalDate latestVisit = entry.getValue().stream()
                    .map(MedicalRecord::getExaminationDate)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            result.add(new DoctorMedicalRecordPatientItemResponse(
                    patient.getId(),
                    patient.getFullName(),
                    patient.getPhone(),
                    patient.getEmail(),
                    patient.getGender(),
                    entry.getValue().size(),
                    latestVisit
            ));
        }

        result.sort(Comparator.comparing(
                DoctorMedicalRecordPatientItemResponse::getLatestVisitDate,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return result;
    }

    public DoctorPatientMedicalRecordsResponse getPatientMedicalRecords(String username, Integer patientId) {
        Doctor doctor = getDoctorByUsername(username);
        List<MedicalRecord> records = medicalRecordRepository
                .findByPatientIdAndDoctorIdOrderByExaminationDateDesc(patientId, doctor.getId());

        if (records.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay benh an cua benh nhan voi bac si hien tai.");
        }

        Patient patient = records.get(0).getPatient();
        List<Integer> recordIds = records.stream().map(MedicalRecord::getId).toList();
        Map<Integer, List<PrescriptionDetail>> medicinesByRecord = prescriptionDetailRepository.findByMedicalRecordIdIn(recordIds).stream()
                .collect(Collectors.groupingBy(detail -> detail.getMedicalRecord().getId()));
        Map<Integer, List<ServiceDetail>> servicesByRecord = serviceDetailRepository.findByMedicalRecordIdIn(recordIds).stream()
                .collect(Collectors.groupingBy(detail -> detail.getMedicalRecord().getId()));

        List<DoctorPatientMedicalRecordsResponse.RecordItem> recordItems = new ArrayList<>();
        for (MedicalRecord record : records) {
            List<DoctorPatientMedicalRecordsResponse.MedicineItem> medicines = medicinesByRecord
                    .getOrDefault(record.getId(), List.of())
                    .stream()
                    .map(detail -> new DoctorPatientMedicalRecordsResponse.MedicineItem(
                            detail.getMedicine() == null ? null : detail.getMedicine().getName(),
                            detail.getQuantity(),
                            detail.getMedicine() == null ? null : detail.getMedicine().getUnit(),
                            detail.getDosage(),
                            detail.getNote()
                    ))
                    .collect(Collectors.toList());

            List<DoctorPatientMedicalRecordsResponse.ServiceItem> services = servicesByRecord
                    .getOrDefault(record.getId(), List.of())
                    .stream()
                    .map(detail -> new DoctorPatientMedicalRecordsResponse.ServiceItem(
                            detail.getMedicalService() == null ? null : detail.getMedicalService().getName(),
                            detail.getMedicalService() == null ? null : detail.getMedicalService().getPrice()
                    ))
                    .collect(Collectors.toList());

            Appointment appointment = record.getAppointment();
            recordItems.add(new DoctorPatientMedicalRecordsResponse.RecordItem(
                    record.getId(),
                    appointment == null ? null : appointment.getId(),
                    record.getExaminationDate(),
                    appointment == null ? TYPE_NEW_EXAM : resolveDisplayType(appointment.getType()),
                    appointment == null ? null : appointment.getSymptoms(),
                    record.getDiagnosis(),
                    record.getDoctorAdvice(),
                    medicines,
                    services
            ));
        }

        recordItems.sort(Comparator.comparing(
                DoctorPatientMedicalRecordsResponse.RecordItem::getExamDate,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return new DoctorPatientMedicalRecordsResponse(
                new DoctorPatientMedicalRecordsResponse.PatientProfile(
                        patient.getId(),
                        patient.getFullName(),
                        patient.getPhone(),
                        patient.getEmail(),
                        patient.getGender(),
                        patient.getDateOfBirth(),
                        patient.getAddress(),
                        patient.getAvatarUrl()
                ),
                recordItems
        );
    }

    @Transactional
    public CreateFollowUpResponse createFollowUp(String username, Integer recordId, CreateFollowUpRequest request) {
        Doctor doctor = getDoctorByUsername(username);
        MedicalRecord record = medicalRecordRepository.findByIdAndDoctorId(recordId, doctor.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay benh an."));

        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu tai kham khong hop le.");
        }
        Appointment followUp = createFollowUpAppointment(
                record.getAppointment(),
                request.getFollowUpDate(),
                request.getFollowUpTime(),
                request.getNote()
        );

        return new CreateFollowUpResponse(
                followUp.getId(),
                followUp.getPatient() == null ? null : followUp.getPatient().getId(),
                followUp.getDoctor() == null ? null : followUp.getDoctor().getId(),
                followUp.getAppointmentDate() == null ? null : followUp.getAppointmentDate().toLocalDate(),
                followUp.getAppointmentDate() == null ? null : followUp.getAppointmentDate().toLocalTime(),
                resolveDisplayType(followUp.getType()),
                resolveDisplayStatus(followUp.getStatus()),
                followUp.getNotes()
        );
    }

    public DoctorWeekScheduleResponse getWeekSchedule(String username, LocalDate startDate) {
        Doctor doctor = getDoctorByUsername(username);
        if (startDate == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu startDate.");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = startDate.plusDays(7).atStartOfDay();
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAsc(doctor.getId(), start, end);

        Map<LocalDate, long[]> counters = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            counters.put(startDate.plusDays(i), new long[]{0, 0});
        }

        for (Appointment appointment : appointments) {
            if (appointment == null || appointment.getAppointmentDate() == null || isCancelledForDoctorFlow(appointment)) {
                continue;
            }
            LocalDate date = appointment.getAppointmentDate().toLocalDate();
            long[] periods = counters.get(date);
            if (periods == null) {
                continue;
            }
            if (appointment.getAppointmentDate().toLocalTime().isBefore(LocalTime.NOON)) {
                periods[0]++;
            } else {
                periods[1]++;
            }
        }

        List<DoctorWeekScheduleResponse.DayScheduleItem> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            long[] periods = counters.getOrDefault(date, new long[]{0, 0});
            days.add(new DoctorWeekScheduleResponse.DayScheduleItem(
                    date,
                    toVietnameseDayName(date.getDayOfWeek()),
                    periods[0],
                    periods[1]
            ));
        }

        LocalDate endDate = startDate.plusDays(6);
        String weekRange = startDate.format(DateTimeFormatter.ofPattern("dd/MM"))
                + " - "
                + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return new DoctorWeekScheduleResponse(weekRange, days);
    }

    public List<DoctorScheduleDayAppointmentResponse> getDaySchedule(
            String username,
            LocalDate date,
            String period
    ) {
        Doctor doctor = getDoctorByUsername(username);
        if (date == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Thieu date.");
        }
        String normalizedPeriod = normalizeText(period);
        if (normalizedPeriod == null || (!normalizedPeriod.equalsIgnoreCase("morning") && !normalizedPeriod.equalsIgnoreCase("afternoon"))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "period chi ho tro morning hoac afternoon.");
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAsc(doctor.getId(), start, end);

        boolean morning = normalizedPeriod.equalsIgnoreCase("morning");
        return appointments.stream()
                .filter(appointment -> appointment != null && appointment.getAppointmentDate() != null)
                .filter(appointment -> !isCancelledForDoctorFlow(appointment))
                .filter(appointment -> morning
                        ? appointment.getAppointmentDate().toLocalTime().isBefore(LocalTime.NOON)
                        : !appointment.getAppointmentDate().toLocalTime().isBefore(LocalTime.NOON))
                .map(appointment -> new DoctorScheduleDayAppointmentResponse(
                        appointment.getId(),
                        appointment.getPatientName(),
                        appointment.getAppointmentDate().toLocalTime(),
                        formatTimeLabel(appointment.getAppointmentDate().toLocalTime()),
                        resolveDisplayType(appointment.getType()),
                        resolveDisplayStatus(appointment.getStatus())
                ))
                .collect(Collectors.toList());
    }

    public DoctorProfileResponse getProfile(String username) {
        Doctor doctor = getDoctorByUsername(username);
        syncDoctorRating(doctor);
        return toDoctorProfileResponse(doctor);
    }

    @Transactional
    public DoctorProfileResponse updateProfile(String username, UpdateDoctorProfileRequest request) {
        Doctor doctor = getDoctorByUsername(username);
        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu ho so khong hop le.");
        }

        if (request.getFullName() != null) {
            String fullName = trimToNull(request.getFullName());
            if (fullName == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Ho ten khong duoc de trong.");
            }
            doctor.setFullName(fullName);
        }
        if (request.getPhone() != null) {
            doctor.setPhone(trimToNull(request.getPhone()));
        }
        if (request.getAddress() != null) {
            doctor.setAddress(trimToNull(request.getAddress()));
        }
        if (request.getBio() != null) {
            doctor.setBio(trimToNull(request.getBio()));
        }
        if (request.getExperienceYears() != null) {
            if (request.getExperienceYears() < 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "So nam kinh nghiem khong duoc am.");
            }
            doctor.setExperienceYears(request.getExperienceYears());
        }

        Doctor saved = doctorRepository.save(doctor);
        syncDoctorRating(saved);
        return toDoctorProfileResponse(saved);
    }

    @Transactional
    public AvatarUploadResponse uploadProfileAvatar(String username, MultipartFile file) {
        DoctorResponse response = doctorService.uploadOwnPhoto(username, file);
        Doctor doctor = getDoctorByUsername(username);
        String avatarUrl = response == null ? null : trimToNull(response.getImageUrl());
        if (avatarUrl == null && doctor.getId() != null && doctorPhotoRepository.findIdByDoctorId(doctor.getId()).isPresent()) {
            avatarUrl = "/api/doctors/" + doctor.getId() + "/photo";
        }
        doctor.setAvatarUrl(avatarUrl);
        doctorRepository.save(doctor);
        return new AvatarUploadResponse(avatarUrl);
    }

    @Transactional
    public AvatarUploadResponse deleteProfileAvatar(String username) {
        Doctor doctor = getDoctorByUsername(username);
        doctorService.deleteOwnPhoto(username);
        doctor.setAvatarUrl(null);
        doctorRepository.save(doctor);
        return new AvatarUploadResponse(null);
    }

    private void createPrescriptionItems(MedicalRecord record, List<CompleteAppointmentRequest.MedicineItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (CompleteAppointmentRequest.MedicineItem item : items) {
            if (item == null || item.getMedicineId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "medicineId khong hop le.");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "So luong thuoc phai lon hon 0.");
            }

            Medicine medicine = medicineRepository.findById(item.getMedicineId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay thuoc ID: " + item.getMedicineId()));

            Integer currentQuantity = medicine.getQuantity() == null ? 0 : medicine.getQuantity();
            if (currentQuantity < item.getQuantity()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "So luong thuoc ton kho khong du: " + safeText(medicine.getName()));
            }

            PrescriptionDetail detail = new PrescriptionDetail();
            detail.setMedicalRecord(record);
            detail.setMedicine(medicine);
            detail.setQuantity(item.getQuantity());
            detail.setDosage(trimToNull(item.getDosage()));
            detail.setNote(trimToNull(item.getNote()));
            prescriptionDetailRepository.save(detail);

            medicine.setQuantity(currentQuantity - item.getQuantity());
            medicineService.applyStockStatus(medicine);
            medicineRepository.save(medicine);
        }
    }

    private void createServiceItems(MedicalRecord record, List<CompleteAppointmentRequest.ServiceItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (CompleteAppointmentRequest.ServiceItem item : items) {
            if (item == null || item.getMedicalServiceId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "medicalServiceId khong hop le.");
            }

            MedicalService medicalService = medicalServiceRepository.findById(item.getMedicalServiceId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay dich vu ID: " + item.getMedicalServiceId()));

            ServiceDetail detail = new ServiceDetail();
            detail.setMedicalRecord(record);
            detail.setMedicalService(medicalService);
            detail.setQuantity(1);
            detail.setResult(trimToNull(item.getNote()));
            serviceDetailRepository.save(detail);
        }
    }

    private Appointment createFollowUpAppointment(
            Appointment sourceAppointment,
            LocalDate followUpDate,
            LocalTime followUpTime,
            String note
    ) {
        if (sourceAppointment == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong tim thay lich hen goc de tao tai kham.");
        }
        if (followUpDate == null || followUpTime == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui long cung cap ngay va gio tai kham.");
        }

        LocalDateTime followUpDateTime = LocalDateTime.of(followUpDate, followUpTime);
        if (followUpDateTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong the tao lich tai kham trong qua khu.");
        }

        Appointment followUp = new Appointment();
        followUp.setPatient(sourceAppointment.getPatient());
        followUp.setDoctor(sourceAppointment.getDoctor());
        followUp.setSpecialty(sourceAppointment.getSpecialty());
        followUp.setMedicalService(sourceAppointment.getMedicalService());
        followUp.setAppointmentDate(followUpDateTime);
        followUp.setType(TYPE_FOLLOW_UP);
        followUp.setStatus(STATUS_PENDING);
        followUp.setPaymentStatus("UNPAID");
        followUp.setNotes(trimToNull(note));
        followUp.setSymptoms(null);
        followUp.setConsultationFee(resolveConsultationFee(sourceAppointment));
        followUp.setAppointmentCode(generateAppointmentCode());
        return appointmentRepository.save(followUp);
    }

    private Double resolveConsultationFee(Appointment sourceAppointment) {
        if (sourceAppointment == null) {
            return null;
        }
        if (sourceAppointment.getConsultationFee() != null) {
            return sourceAppointment.getConsultationFee();
        }
        Doctor doctor = sourceAppointment.getDoctor();
        BigDecimal price = doctor == null ? null : doctor.getPrice();
        return price == null ? null : price.doubleValue();
    }

    private String generateAppointmentCode() {
        String code;
        do {
            code = "PKB-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000);
        } while (appointmentRepository.existsByAppointmentCode(code));
        return code;
    }

    private DoctorProfileResponse toDoctorProfileResponse(Doctor doctor) {
        String specialtyName = doctor.getSpecialty() == null ? null : doctor.getSpecialty().getName();
        String avatarUrl = trimToNull(doctor.getAvatarUrl());
        if (avatarUrl == null && doctor.getId() != null && doctorPhotoRepository.findIdByDoctorId(doctor.getId()).isPresent()) {
            avatarUrl = "/api/doctors/" + doctor.getId() + "/photo";
        }

        return new DoctorProfileResponse(
                doctor.getId(),
                doctor.getFullName(),
                doctor.getEmail(),
                doctor.getPhone(),
                doctor.getAddress(),
                doctor.getSpecialty() == null ? null : doctor.getSpecialty().getId(),
                specialtyName,
                doctor.getExperienceYears() == null ? 0 : doctor.getExperienceYears(),
                doctor.getBio(),
                avatarUrl,
                doctor.getCreatedAt() == null ? null : doctor.getCreatedAt().toLocalDate(),
                doctor.getRating() == null ? 0 : doctor.getRating(),
                specialtyName == null ? null : "Làm việc tại: Khoa " + specialtyName + " MedCare"
        );
    }

    private void syncDoctorRating(Doctor doctor) {
        if (doctor == null || doctor.getId() == null) {
            return;
        }
        Double avg = feedbackRepository.findAverageRatingByDoctorId(doctor.getId());
        double normalized = avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0;
        if (doctor.getRating() == null || Double.compare(doctor.getRating(), normalized) != 0) {
            doctor.setRating(normalized);
            doctorRepository.save(doctor);
        }
    }

    private DoctorAppointmentListItemResponse toAppointmentListItem(Appointment appointment) {
        LocalDateTime dateTime = appointment.getAppointmentDate();
        String displayStatus = resolveDisplayStatus(appointment.getStatus());
        boolean canExamine = DISPLAY_STATUS_PENDING.equals(displayStatus);
        return new DoctorAppointmentListItemResponse(
                appointment.getId(),
                appointment.getPatient() == null ? null : appointment.getPatient().getId(),
                appointment.getPatientName(),
                appointment.getPatient() == null ? null : appointment.getPatient().getPhone(),
                appointment.getPatient() == null ? null : appointment.getPatient().getEmail(),
                dateTime == null ? null : dateTime.toLocalDate(),
                dateTime == null ? null : dateTime.toLocalTime(),
                formatTimeLabel(dateTime == null ? null : dateTime.toLocalTime()),
                resolveDisplayType(appointment.getType()),
                displayStatus,
                canExamine
        );
    }

    private boolean matchesKeyword(Appointment appointment, String keyword) {
        if (keyword == null) {
            return true;
        }
        Patient patient = appointment.getPatient();
        return containsFolded(patient == null ? null : patient.getFullName(), keyword)
                || containsFolded(patient == null ? null : patient.getPhone(), keyword)
                || containsFolded(patient == null ? null : patient.getEmail(), keyword);
    }

    private boolean matchesPatientKeyword(Patient patient, String keyword) {
        if (keyword == null) {
            return true;
        }
        return containsFolded(patient == null ? null : patient.getFullName(), keyword)
                || containsFolded(patient == null ? null : patient.getPhone(), keyword)
                || containsFolded(patient == null ? null : patient.getEmail(), keyword);
    }

    private boolean matchesStatusFilter(Appointment appointment, AppointmentStatusFilter filter) {
        if (filter == AppointmentStatusFilter.ALL) {
            return true;
        }
        AppointmentStatusFilter appointmentStatus = resolveStatusFilterFromStoredValue(appointment.getStatus());
        return appointmentStatus == filter;
    }

    private boolean matchesDateFilter(Appointment appointment, LocalDate date) {
        if (date == null) {
            return true;
        }
        return appointment.getAppointmentDate() != null && date.equals(appointment.getAppointmentDate().toLocalDate());
    }

    private boolean matchesTypeFilter(Appointment appointment, AppointmentTypeFilter filter) {
        if (filter == AppointmentTypeFilter.ALL) {
            return true;
        }
        String normalizedType = foldText(resolveDisplayType(appointment.getType()));
        if (filter == AppointmentTypeFilter.NEW_EXAM) {
            return normalizedType != null && normalizedType.contains("khambenh");
        }
        return normalizedType != null && normalizedType.contains("taikham");
    }

    private AppointmentStatusFilter parseStatusFilter(String status) {
        String normalized = foldText(status);
        if (normalized == null) {
            return AppointmentStatusFilter.ALL;
        }
        if (normalized.contains("chokham") || normalized.contains("dangcho") || normalized.contains("pending")) {
            return AppointmentStatusFilter.PENDING;
        }
        if (normalized.contains("dakham") || normalized.contains("completed")) {
            return AppointmentStatusFilter.COMPLETED;
        }
        if (normalized.contains("huylich") || normalized.contains("cancel")) {
            return AppointmentStatusFilter.CANCELLED;
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "Trang thai loc khong hop le.");
    }

    private AppointmentTypeFilter parseTypeFilter(String type) {
        String normalized = foldText(type);
        if (normalized == null) {
            return AppointmentTypeFilter.ALL;
        }
        if (normalized.contains("khambenh")) {
            return AppointmentTypeFilter.NEW_EXAM;
        }
        if (normalized.contains("taikham")) {
            return AppointmentTypeFilter.FOLLOW_UP;
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "Loai kham loc khong hop le.");
    }

    private AppointmentStatusFilter resolveStatusFilterFromStoredValue(String status) {
        String normalized = foldText(status);
        if (normalized == null) {
            return AppointmentStatusFilter.PENDING;
        }
        if (normalized.contains("completed") || normalized.contains("dakham")) {
            return AppointmentStatusFilter.COMPLETED;
        }
        if (normalized.contains("cancel") || normalized.contains("huylich")) {
            return AppointmentStatusFilter.CANCELLED;
        }
        return AppointmentStatusFilter.PENDING;
    }

    private boolean isPendingForDoctorFlow(Appointment appointment) {
        return resolveStatusFilterFromStoredValue(appointment == null ? null : appointment.getStatus()) == AppointmentStatusFilter.PENDING;
    }

    private boolean isCompletedForDoctorFlow(Appointment appointment) {
        return resolveStatusFilterFromStoredValue(appointment == null ? null : appointment.getStatus()) == AppointmentStatusFilter.COMPLETED;
    }

    private boolean isCancelledForDoctorFlow(Appointment appointment) {
        return resolveStatusFilterFromStoredValue(appointment == null ? null : appointment.getStatus()) == AppointmentStatusFilter.CANCELLED;
    }

    private String resolveDisplayStatus(String storedStatus) {
        AppointmentStatusFilter filter = resolveStatusFilterFromStoredValue(storedStatus);
        return switch (filter) {
            case COMPLETED -> DISPLAY_STATUS_COMPLETED;
            case CANCELLED -> DISPLAY_STATUS_CANCELLED;
            default -> DISPLAY_STATUS_PENDING;
        };
    }

    private String resolveDisplayType(String storedType) {
        String normalized = foldText(storedType);
        if (normalized == null) {
            return TYPE_NEW_EXAM;
        }
        if (normalized.contains("taikham")) {
            return TYPE_FOLLOW_UP;
        }
        return TYPE_NEW_EXAM;
    }

    private Doctor getDoctorByUsername(String username) {
        return doctorRepository.findByAccount_Username(username)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan doctor chua duoc lien ket voi ho so bac si."
                ));
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String trimToNull(String value) {
        return normalizeText(value);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String formatTimeLabel(LocalTime time) {
        return time == null ? null : time.format(TIME_LABEL_FORMATTER);
    }

    private boolean containsFolded(String source, String foldedKeyword) {
        String foldedSource = foldText(source);
        return foldedSource != null && foldedKeyword != null && foldedSource.contains(foldedKeyword);
    }

    private String foldText(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        String noAccent = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccent
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private double resolveSatisfactionRate(Doctor doctor) {
        if (doctor == null || doctor.getId() == null) {
            return 0;
        }
        Double average = feedbackRepository.findAverageRatingByDoctorId(doctor.getId());
        if (average == null) {
            if (doctor.getRating() == null || doctor.getRating() != 0.0) {
                doctor.setRating(0.0);
                doctorRepository.save(doctor);
            }
            return 0;
        }
        return Math.round(average * 10.0) / 10.0;
    }

    private String toVietnameseDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Thứ 2";
            case TUESDAY -> "Thứ 3";
            case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5";
            case FRIDAY -> "Thứ 6";
            case SATURDAY -> "Thứ 7";
            case SUNDAY -> "Chủ Nhật";
        };
    }

    private enum AppointmentStatusFilter {
        ALL,
        PENDING,
        COMPLETED,
        CANCELLED
    }

    private enum AppointmentTypeFilter {
        ALL,
        NEW_EXAM,
        FOLLOW_UP
    }
}
