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
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorPortalService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String DISPLAY_STATUS_PENDING = "Chá» khÃ¡m";
    private static final String DISPLAY_STATUS_COMPLETED = "ÄÃ£ khÃ¡m";
    private static final String DISPLAY_STATUS_CANCELLED = "Há»§y lá»‹ch";

    private static final String TYPE_NEW_EXAM = "KhÃ¡m bá»‡nh";
    private static final String TYPE_FOLLOW_UP = "TÃ¡i khÃ¡m";

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

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    private static final String PAYMENT_STATUS_UNPAID = "UNPAID";
    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String PAYMENT_STATUS_PAID_ONLINE = "PAID_ONLINE";
    private static final String FOLLOW_UP_VALIDATION_CODE = "FOLLOW_UP_VALIDATION_ERROR";

    private static final DateTimeFormatter FOLLOW_UP_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter FOLLOW_UP_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
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
                resolveDisplayType(appointment),
                resolveTypeCode(appointment),
                resolveDisplayStatus(appointment.getStatus()),
                resolvePaymentStatusDisplay(appointment.getPaymentStatus()),
                appointment.getConsultationFee(),
                resolveAppointmentNoteForDetail(appointment),
                resolveAppointmentSymptomsForDetail(appointment),
                resolveFollowUpNote(appointment),
                resolveParentAppointmentId(appointment),
                isFollowUpAppointment(appointment)
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
        if (isCancelledForDoctorFlow(appointment)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "KhÃ´ng thá»ƒ khÃ¡m lá»‹ch háº¹n Ä‘Ã£ bá»‹ há»§y.");
        }
        if (isCompletedForDoctorFlow(appointment) || medicalRecordRepository.existsByAppointmentId(appointment.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen da duoc hoan tat, khong the complete lai.");
        }

        String appointmentType = resolveDisplayType(appointment.getAppointmentType());
        appointment.setAppointmentType(appointmentType);
        String requestSymptoms = trimToNull(request.getSymptoms());
        if (requestSymptoms != null) {
            appointment.setSymptoms(requestSymptoms);
        }
        appointment.setConsultationFee(resolveConsultationFeeByType(appointmentType, doctor));
        appointmentRepository.save(appointment);

        MedicalRecord record = new MedicalRecord();
        record.setAppointment(appointment);
        record.setDoctor(doctor);
        record.setPatient(appointment.getPatient());
        record.setType(appointmentType);
        record.setExaminationDate(
                appointment.getAppointmentDate() == null
                        ? LocalDate.now()
                        : appointment.getAppointmentDate().toLocalDate()
        );
        String diagnosis = trimToNull(request.getDiagnosis());
        record.setDiagnosis(diagnosis == null ? "Chua cap nhat chan doan." : diagnosis);
        record.setDoctorAdvice(trimToNull(request.getDoctorAdvice()));
        record.setTreatmentPlan(null);
        record.setPrescription(null);
        record.setMedicalRecordCode(generateMedicalRecordCode());
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(LocalDateTime.now());
        }
        MedicalRecord savedRecord = medicalRecordRepository.save(record);

        createPrescriptionItems(savedRecord, request.getMedicineItems());
        createServiceItems(savedRecord, request.getServiceItems());
        Invoice savedInvoice = invoiceService.createInvoiceFromRecord(savedRecord);

        appointment.setStatus(STATUS_COMPLETED);
        appointmentRepository.save(appointment);

        CompleteAppointmentResponse.FollowUpAppointmentInfo followUpAppointmentInfo = null;
        CompleteAppointmentRequest.FollowUp followUp = request.getFollowUp();
        if (followUp != null && Boolean.TRUE.equals(followUp.getNeedFollowUp())) {
            LocalDate followUpDate = parseFlexibleDate(followUp.getFollowUpDate(), "ngay tai kham");
            LocalTime followUpTime = parseFlexibleTime(followUp.getFollowUpTime(), "gio tai kham");
            Appointment followUpAppointment = createFollowUpAppointment(
                    appointment,
                    followUpDate,
                    followUpTime,
                    followUp.getNote()
            );
            followUpAppointmentInfo = toFollowUpAppointmentInfo(followUpAppointment);
            savedRecord.setFollowUpAppointment(followUpAppointment);
            medicalRecordRepository.save(savedRecord);
        }

        CompleteAppointmentResponse.InvoiceInfo invoiceInfo = toCompleteInvoiceInfo(savedInvoice);
        String message = followUpAppointmentInfo == null
                ? "Ho\u00e0n t\u1ea5t kh\u00e1m th\u00e0nh c\u00f4ng"
                : "Ho\u00e0n t\u1ea5t kh\u00e1m, t\u1ea1o h\u00f3a \u0111\u01a1n v\u00e0 t\u1ea1o l\u1ecbch t\u00e1i kh\u00e1m th\u00e0nh c\u00f4ng";

        return new CompleteAppointmentResponse(
                message,
                appointment.getId(),
                appointmentType,
                resolveDisplayStatus(appointment.getStatus()),
                invoiceInfo,
                followUpAppointmentInfo
        );
    }

    public List<DoctorMedicineResponse> getMedicinesForDoctor(String username) {
        return getMedicinesForDoctor(username, null, null, null, null);
    }

    public List<DoctorMedicineResponse> getMedicinesForDoctor(
            String username,
            String keyword,
            Integer categoryId,
            String status,
            String category
    ) {
        getDoctorByUsername(username);
        return medicineService.getAllMedicines(
                        trimToNull(keyword),
                        categoryId,
                        trimToNull(status),
                        trimToNull(category)
                ).stream()
                .map(medicine -> {
                    String resolvedStatus = medicineService.resolveStockStatus(medicine.getUnit(), medicine.getQuantity());
                    String categoryName = medicineService.toMedicineResponse(medicine).getMedicineCategoryName();
                    return new DoctorMedicineResponse(
                            medicine.getId(),
                            medicine.getName(),
                            medicine.getMedicineCategory() == null ? null : medicine.getMedicineCategory().getId(),
                            categoryName,
                            categoryName,
                            medicine.getUnit(),
                            medicine.getQuantity(),
                            medicine.getDosage(),
                            medicine.getPrice(),
                            resolvedStatus
                    );
                })
                .filter(item -> "C\u00f2n h\u00e0ng".equals(item.getStatus()) || "S\u1eafp h\u1ebft".equals(item.getStatus()))
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
        Set<Integer> patientIdsWithRecords = extractPatientIdsWithRecords(records);

        if (patientIdsWithRecords.isEmpty()) {
            return new DoctorMedicalRecordsSummaryResponse(0, 0, 0);
        }

        Map<Integer, PatientVisitStats> visitStatsByPatient = buildPatientVisitStats(
                appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctor.getId()),
                records,
                patientIdsWithRecords
        );

        long totalPatients = patientIdsWithRecords.size();
        long newPatients = patientIdsWithRecords.stream()
                .map(visitStatsByPatient::get)
                .filter(Objects::nonNull)
                .filter(stats -> stats.newExamCount() > 0)
                .count();
        long followUpPatients = patientIdsWithRecords.stream()
                .map(visitStatsByPatient::get)
                .filter(Objects::nonNull)
                .filter(stats -> stats.followUpCount() > 0)
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
        Map<Integer, PatientVisitStats> visitStatsByPatient = buildPatientVisitStats(
                appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctor.getId()),
                records,
                recordsByPatient.keySet()
        );

        List<DoctorMedicalRecordPatientItemResponse> result = new ArrayList<>();
        for (Map.Entry<Integer, List<MedicalRecord>> entry : recordsByPatient.entrySet()) {
            Patient patient = entry.getValue().get(0).getPatient();
            if (!matchesPatientKeyword(patient, normalizedKeyword)) {
                continue;
            }

            LocalDate latestRecordDate = entry.getValue().stream()
                    .map(MedicalRecord::getExaminationDate)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);
            PatientVisitStats visitStats = visitStatsByPatient.getOrDefault(
                    patient.getId(),
                    new PatientVisitStats(0, 0, latestRecordDate)
            );

            result.add(new DoctorMedicalRecordPatientItemResponse(
                    patient.getId(),
                    patient.getFullName(),
                    patient.getPhone(),
                    patient.getEmail(),
                    patient.getGender(),
                    visitStats.newExamCount(),
                    visitStats.followUpCount(),
                    visitStats.totalVisitCount(),
                    visitStats.latestVisitDate() == null ? latestRecordDate : visitStats.latestVisitDate()
            ));
        }

        result.sort(Comparator.comparing(
                DoctorMedicalRecordPatientItemResponse::getLatestVisitDate,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return result;
    }

    private Set<Integer> extractPatientIdsWithRecords(List<MedicalRecord> records) {
        return records.stream()
                .map(MedicalRecord::getPatient)
                .filter(Objects::nonNull)
                .map(Patient::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Integer, PatientVisitStats> buildPatientVisitStats(
            List<Appointment> appointments,
            List<MedicalRecord> fallbackRecords,
            Set<Integer> patientIds
    ) {
        if (patientIds == null || patientIds.isEmpty()) {
            return Map.of();
        }

        Map<Integer, Long> newExamCounts = new HashMap<>();
        Map<Integer, Long> followUpCounts = new HashMap<>();
        Map<Integer, LocalDate> latestVisitDates = new HashMap<>();
        Set<Integer> patientsCountedFromAppointments = new HashSet<>();

        for (Appointment appointment : appointments) {
            Integer patientId = appointment == null || appointment.getPatient() == null
                    ? null
                    : appointment.getPatient().getId();
            if (patientId == null || !patientIds.contains(patientId) || isCancelledForDoctorFlow(appointment)) {
                continue;
            }

            if (isFollowUpAppointment(appointment)) {
                followUpCounts.merge(patientId, 1L, Long::sum);
            } else {
                newExamCounts.merge(patientId, 1L, Long::sum);
            }

            patientsCountedFromAppointments.add(patientId);
            mergeLatestVisitDate(
                    latestVisitDates,
                    patientId,
                    appointment.getAppointmentDate() == null ? null : appointment.getAppointmentDate().toLocalDate()
            );
        }

        for (MedicalRecord record : fallbackRecords) {
            Integer patientId = record == null || record.getPatient() == null
                    ? null
                    : record.getPatient().getId();
            if (patientId == null || !patientIds.contains(patientId)) {
                continue;
            }

            if (!patientsCountedFromAppointments.contains(patientId)) {
                if (isFollowUpRecord(record)) {
                    followUpCounts.merge(patientId, 1L, Long::sum);
                } else {
                    newExamCounts.merge(patientId, 1L, Long::sum);
                }
            }

            mergeLatestVisitDate(latestVisitDates, patientId, record.getExaminationDate());
        }

        Map<Integer, PatientVisitStats> statsByPatient = new HashMap<>();
        for (Integer patientId : patientIds) {
            long newExamCount = newExamCounts.getOrDefault(patientId, 0L);
            long followUpCount = followUpCounts.getOrDefault(patientId, 0L);
            statsByPatient.put(
                    patientId,
                    new PatientVisitStats(newExamCount, followUpCount, latestVisitDates.get(patientId))
            );
        }
        return statsByPatient;
    }

    private boolean isFollowUpRecord(MedicalRecord record) {
        if (record == null) {
            return false;
        }
        if (record.getAppointment() != null) {
            return isFollowUpAppointment(record.getAppointment());
        }
        return isFollowUpType(record.getType());
    }

    private LocalDate resolveLatestDate(LocalDate current, LocalDate candidate) {
        if (current == null) {
            return candidate;
        }
        if (candidate == null) {
            return current;
        }
        return candidate.isAfter(current) ? candidate : current;
    }

    private void mergeLatestVisitDate(Map<Integer, LocalDate> latestVisitDates, Integer patientId, LocalDate candidate) {
        if (candidate == null) {
            return;
        }
        latestVisitDates.merge(patientId, candidate, this::resolveLatestDate);
    }

    @Transactional(readOnly = true)
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
                    resolveRecordCreatedAt(record),
                    record.getExaminationDate(),
                    appointment == null ? "Kh\u00e1m b\u1ec7nh" : resolveDisplayType(appointment),
                    appointment == null ? "NEW_EXAM" : resolveTypeCode(appointment),
                    appointment == null ? null : resolveAppointmentSymptomsForDetail(appointment),
                    record.getDiagnosis(),
                    record.getDoctorAdvice(),
                    medicines,
                    services,
                    toDoctorFollowUpAppointmentInfo(record.getFollowUpAppointment())
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
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> buildFollowUpValidationException("Khong tim thay benh an."));

        if (record.getDoctor() == null || record.getDoctor().getId() == null
                || !doctor.getId().equals(record.getDoctor().getId())) {
            throw buildFollowUpValidationException("Benh an nay khong thuoc bac si hien tai.");
        }

        Appointment sourceAppointment = record.getAppointment();
        if (sourceAppointment == null) {
            throw buildFollowUpValidationException("Benh an khong hop le de tao lich tai kham.");
        }
        if (sourceAppointment.getId() == null) {
            throw buildFollowUpValidationException("Benh an khong hop le de tao lich tai kham.");
        }
        if (record.getFollowUpAppointment() != null || appointmentRepository.existsByParentAppointmentId(sourceAppointment.getId())) {
            throw buildFollowUpValidationException("Benh an nay da ton tai lich tai kham.");
        }

        FollowUpRequestPayload payload = validateCreateFollowUpRequest(request);
        Appointment followUp = createFollowUpAppointment(
                sourceAppointment,
                payload.followUpDate(),
                payload.followUpTime(),
                payload.note()
        );
        record.setFollowUpAppointment(followUp);
        medicalRecordRepository.save(record);

        return new CreateFollowUpResponse(
                followUp.getId(),
                followUp.getAppointmentDate() == null ? null : followUp.getAppointmentDate().toLocalDate(),
                followUp.getAppointmentDate() == null ? null : followUp.getAppointmentDate().toLocalTime(),
                resolveDisplayType(followUp.getAppointmentType()),
                resolveDisplayStatus(followUp.getStatus()),
                resolvePaymentStatusDisplay(followUp.getPaymentStatus()),
                followUp.getConsultationFee()
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
                        resolveDisplayType(appointment),
                        resolveTypeCode(appointment),
                        resolveDisplayStatus(appointment.getStatus()),
                        isFollowUpAppointment(appointment),
                        resolveParentAppointmentId(appointment)
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
                // FE co the gui dong thuoc placeholder chua chon thuoc.
                continue;
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
                // FE co the gui item rong neu chua tick dich vu.
                continue;
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
        if (sourceAppointment.getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich hen goc khong hop le.");
        }
        if (isCancelledForDoctorFlow(sourceAppointment)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong the tao tai kham cho lich da huy.");
        }
        if (followUpDate == null || followUpTime == null) {
            throw buildFollowUpValidationException(
                    "Vui long cung cap ngay va gio tai kham.",
                    Map.of(
                            "followUpDate", "Vui long cung cap followUpDate theo dinh dang yyyy-MM-dd.",
                            "followUpTime", "Vui long cung cap followUpTime theo dinh dang HH:mm."
                    )
            );
        }
        if (appointmentRepository.existsByParentAppointmentId(sourceAppointment.getId())) {
            throw buildFollowUpValidationException("Da ton tai lich tai kham cho benh an nay.");
        }

        if (followUpDate.isBefore(LocalDate.now())) {
            throw buildFollowUpValidationException(
                    "Ngay tai kham khong duoc o qua khu.",
                    Map.of("followUpDate", "Ngay tai kham khong duoc o qua khu.")
            );
        }
        SlotRule slotRule = resolveFollowUpSlotRule(followUpDate, followUpTime);
        LocalDateTime followUpDateTime = slotRule.start();
        if (followUpDateTime.isBefore(LocalDateTime.now())) {
            throw buildFollowUpValidationException(
                    "Thoi gian tai kham khong duoc o qua khu.",
                    Map.of("followUpTime", "Thoi gian tai kham khong duoc o qua khu.")
            );
        }
        Integer doctorId = sourceAppointment.getDoctor() == null ? null : sourceAppointment.getDoctor().getId();
        validateDoctorScheduleForFollowUp(doctorId, slotRule);
        validateDoctorAvailabilityForFollowUp(doctorId, slotRule);

        Appointment followUp = new Appointment();
        followUp.setPatient(sourceAppointment.getPatient());
        followUp.setDoctor(sourceAppointment.getDoctor());
        followUp.setSpecialty(sourceAppointment.getSpecialty());
        followUp.setMedicalService(null);
        followUp.setServicePackage(null);
        followUp.setParentAppointment(sourceAppointment);
        followUp.setAppointmentDate(followUpDateTime);
        followUp.setAppointmentType("T\u00e1i kh\u00e1m");
        followUp.setStatus(STATUS_PENDING);
        followUp.setPaymentStatus(PAYMENT_STATUS_UNPAID);
        followUp.setFollowUpNote(trimToNull(note));
        followUp.setNotes(null);
        followUp.setSymptoms(resolveSeedSymptomsForFollowUp(sourceAppointment));
        followUp.setConsultationFee(resolveConsultationFeeByType("T\u00e1i kh\u00e1m", sourceAppointment.getDoctor()));
        followUp.setAppointmentCode(generateAppointmentCode());
        return appointmentRepository.save(followUp);
    }

    private Double resolveConsultationFeeByType(String appointmentType, Doctor doctor) {
        BigDecimal doctorPrice = doctor == null ? null : doctor.getPrice();
        if (doctorPrice == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Bac si chua duoc cau hinh phi kham.");
        }
        double baseFee = doctorPrice.doubleValue();
        if (isFollowUpType(appointmentType)) {
            return baseFee * 0.5;
        }
        return baseFee;
    }

    private boolean isFollowUpType(String type) {
        String normalized = foldText(type);
        return normalized != null && normalized.contains("taikham");
    }

    private boolean isFollowUpAppointment(Appointment appointment) {
        if (appointment == null) {
            return false;
        }
        if (isFollowUpType(appointment.getAppointmentType())) {
            return true;
        }
        if (resolveParentAppointmentId(appointment) != null) {
            return true;
        }
        return trimToNull(appointment.getFollowUpNote()) != null;
    }

    private Integer resolveParentAppointmentId(Appointment appointment) {
        if (appointment == null || appointment.getParentAppointment() == null) {
            return null;
        }
        return appointment.getParentAppointment().getId();
    }

    private String resolveTypeCode(Appointment appointment) {
        return isFollowUpAppointment(appointment) ? "FOLLOW_UP" : "NEW_EXAM";
    }

    private String resolveFollowUpNote(Appointment appointment) {
        if (!isFollowUpAppointment(appointment)) {
            return null;
        }
        String followUpNote = trimToNull(appointment.getFollowUpNote());
        if (followUpNote != null) {
            return followUpNote;
        }
        return trimToNull(appointment.getNotes());
    }

    private String resolveAppointmentNoteForDetail(Appointment appointment) {
        if (appointment == null || isFollowUpAppointment(appointment)) {
            return null;
        }
        return trimToNull(appointment.getNotes());
    }

    private String resolveAppointmentSymptomsForDetail(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        String symptoms = trimToNull(appointment.getSymptoms());
        if (symptoms != null) {
            return symptoms;
        }
        if (!isFollowUpAppointment(appointment)) {
            return null;
        }

        Integer parentAppointmentId = resolveParentAppointmentId(appointment);
        if (parentAppointmentId == null) {
            return null;
        }
        return appointmentRepository.findById(parentAppointmentId)
                .map(Appointment::getSymptoms)
                .map(this::trimToNull)
                .orElse(null);
    }

    private String resolveSeedSymptomsForFollowUp(Appointment sourceAppointment) {
        if (sourceAppointment == null) {
            return null;
        }
        return trimToNull(sourceAppointment.getSymptoms());
    }

    private FollowUpRequestPayload validateCreateFollowUpRequest(CreateFollowUpRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (request == null) {
            fieldErrors.put("followUpDate", "Vui long cung cap followUpDate theo dinh dang yyyy-MM-dd.");
            fieldErrors.put("followUpTime", "Vui long cung cap followUpTime theo dinh dang HH:mm.");
            throw buildFollowUpValidationException("Du lieu tai kham khong hop le.", fieldErrors);
        }

        LocalDate followUpDate = parseFollowUpDate(request.getFollowUpDate(), fieldErrors);
        LocalTime followUpTime = parseFollowUpTime(request.getFollowUpTime(), fieldErrors);
        if (!fieldErrors.isEmpty()) {
            throw buildFollowUpValidationException("Du lieu tai kham khong hop le.", fieldErrors);
        }

        return new FollowUpRequestPayload(followUpDate, followUpTime, trimToNull(request.getNote()));
    }

    private LocalDate parseFollowUpDate(String rawValue, Map<String, String> fieldErrors) {
        String value = trimToNull(rawValue);
        if (value == null) {
            fieldErrors.put("followUpDate", "Vui long cung cap followUpDate theo dinh dang yyyy-MM-dd.");
            return null;
        }
        try {
            return LocalDate.parse(value, FOLLOW_UP_DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            fieldErrors.put("followUpDate", "followUpDate phai theo dinh dang yyyy-MM-dd.");
            return null;
        }
    }

    private LocalTime parseFollowUpTime(String rawValue, Map<String, String> fieldErrors) {
        String value = trimToNull(rawValue);
        if (value == null) {
            fieldErrors.put("followUpTime", "Vui long cung cap followUpTime theo dinh dang HH:mm.");
            return null;
        }
        try {
            return LocalTime.parse(value, FOLLOW_UP_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            fieldErrors.put("followUpTime", "followUpTime phai theo dinh dang HH:mm.");
            return null;
        }
    }

    private SlotRule resolveFollowUpSlotRule(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            throw buildFollowUpValidationException(
                    "Khung gio tai kham khong hop le.",
                    Map.of("followUpTime", "Vui long cung cap followUpTime theo dinh dang HH:mm.")
            );
        }

        LocalDateTime followUpDateTime = LocalDateTime.of(date, time).withSecond(0).withNano(0);
        for (SlotRule slotRule : buildFollowUpSlotRules(date)) {
            if (slotRule.start().equals(followUpDateTime)) {
                return slotRule;
            }
        }

        throw buildFollowUpValidationException(
                "Khung gio tai kham khong hop le.",
                Map.of(
                        "followUpTime",
                        "Gio hop le: 07:30, 08:00, 09:00, 10:00, 12:30, 13:00, 14:00, 15:00."
                )
        );
    }

    private List<SlotRule> buildFollowUpSlotRules(LocalDate date) {
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

    private void validateDoctorScheduleForFollowUp(Integer doctorId, SlotRule slotRule) {
        if (doctorId == null || slotRule == null) {
            throw buildFollowUpValidationException("Khong xac dinh duoc bac si de tao lich tai kham.");
        }

        if (doctorScheduleRepository.countByDoctorId(doctorId) == 0) {
            return;
        }

        List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorIdAndWorkDate(doctorId, slotRule.date());
        if (schedules.isEmpty()) {
            throw buildFollowUpValidationException(
                    "Bac si khong co lich lam viec vao ngay tai kham da chon.",
                    Map.of("followUpDate", "Bac si khong co lich lam viec vao ngay nay.")
            );
        }

        boolean matchesShift = schedules.stream()
                .map(DoctorSchedule::getShift)
                .map(this::normalizeScheduleShift)
                .anyMatch(shift -> "ALL_DAY".equals(shift) || slotRule.shift().equals(shift));
        if (!matchesShift) {
            throw buildFollowUpValidationException(
                    "Bac si khong co lich lam viec trong khung gio tai kham da chon.",
                    Map.of("followUpTime", "Bac si khong co lich lam viec trong buoi cua gio nay.")
            );
        }
    }

    private void validateDoctorAvailabilityForFollowUp(Integer doctorId, SlotRule slotRule) {
        if (doctorId == null || slotRule == null) {
            throw buildFollowUpValidationException("Khong xac dinh duoc bac si de tao lich tai kham.");
        }

        long appointmentCount = appointmentRepository.countByDoctorInSlot(doctorId, slotRule.start(), slotRule.end());
        if (appointmentCount >= slotRule.maxPatients()) {
            throw buildFollowUpValidationException(
                    "Khung gio tai kham da full.",
                    Map.of("followUpTime", "Khung gio nay da du so luong benh nhan toi da.")
            );
        }
    }

    private String normalizeScheduleShift(String shift) {
        if (shift == null) {
            return "";
        }
        return shift.trim().toUpperCase(Locale.ROOT);
    }

    private BusinessException buildFollowUpValidationException(String message) {
        return buildFollowUpValidationException(message, Map.of());
    }

    private BusinessException buildFollowUpValidationException(String message, Map<String, String> fieldErrors) {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                message,
                FOLLOW_UP_VALIDATION_CODE,
                fieldErrors
        );
    }

    private String generateAppointmentCode() {
        String code;
        do {
            code = "PKB-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000);
        } while (appointmentRepository.existsByAppointmentCode(code));
        return code;
    }

    private LocalDateTime resolveRecordCreatedAt(MedicalRecord record) {
        if (record == null) {
            return null;
        }
        if (record.getCreatedAt() != null) {
            return record.getCreatedAt();
        }
        Appointment appointment = record.getAppointment();
        if (appointment != null && appointment.getAppointmentDate() != null) {
            return appointment.getAppointmentDate();
        }
        return record.getExaminationDate() == null ? null : record.getExaminationDate().atStartOfDay();
    }

    private String generateMedicalRecordCode() {
        String code;
        do {
            code = "BA-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000);
        } while (medicalRecordRepository.existsByMedicalRecordCode(code));
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
                specialtyName == null ? null : "LÃ m viá»‡c táº¡i: Khoa " + specialtyName + " MedCare"
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
        boolean canExamine = "Ch\u01b0a kh\u00e1m".equals(displayStatus);
        return new DoctorAppointmentListItemResponse(
                appointment.getId(),
                appointment.getPatient() == null ? null : appointment.getPatient().getId(),
                appointment.getPatientName(),
                appointment.getPatient() == null ? null : appointment.getPatient().getPhone(),
                appointment.getPatient() == null ? null : appointment.getPatient().getEmail(),
                dateTime == null ? null : dateTime.toLocalDate(),
                dateTime == null ? null : dateTime.toLocalTime(),
                formatTimeLabel(dateTime == null ? null : dateTime.toLocalTime()),
                resolveDisplayType(appointment),
                resolveTypeCode(appointment),
                displayStatus,
                appointment.getConsultationFee(),
                resolvePaymentStatusDisplay(appointment.getPaymentStatus()),
                resolveFollowUpNote(appointment),
                resolveParentAppointmentId(appointment),
                canExamine,
                isFollowUpAppointment(appointment)
        );
    }

    private CompleteAppointmentResponse.InvoiceInfo toCompleteInvoiceInfo(Invoice invoice) {
        if (invoice == null) {
            return null;
        }
        return new CompleteAppointmentResponse.InvoiceInfo(
                invoice.getId(),
                invoice.getConsultationFee(),
                invoice.getMedicineFee(),
                invoice.getServiceFee(),
                invoice.getTotalAmount(),
                resolvePaymentStatusDisplay(invoice.getStatus())
        );
    }

    private CompleteAppointmentResponse.FollowUpAppointmentInfo toFollowUpAppointmentInfo(Appointment followUpAppointment) {
        if (followUpAppointment == null) {
            return null;
        }
        return new CompleteAppointmentResponse.FollowUpAppointmentInfo(
                followUpAppointment.getId(),
                followUpAppointment.getAppointmentDate() == null ? null : followUpAppointment.getAppointmentDate().toLocalDate(),
                followUpAppointment.getAppointmentDate() == null ? null : followUpAppointment.getAppointmentDate().toLocalTime(),
                resolveDisplayType(followUpAppointment),
                resolveDisplayStatus(followUpAppointment.getStatus()),
                followUpAppointment.getConsultationFee(),
                resolvePaymentStatusDisplay(followUpAppointment.getPaymentStatus()),
                resolveFollowUpNote(followUpAppointment),
                resolveParentAppointmentId(followUpAppointment)
        );
    }

    private DoctorPatientMedicalRecordsResponse.FollowUpAppointmentInfo toDoctorFollowUpAppointmentInfo(Appointment followUpAppointment) {
        if (followUpAppointment == null) {
            return null;
        }
        String status = followUpAppointment.getStatus();
        return new DoctorPatientMedicalRecordsResponse.FollowUpAppointmentInfo(
                followUpAppointment.getId(),
                followUpAppointment.getAppointmentCode(),
                followUpAppointment.getAppointmentDate(),
                resolveDisplayType(followUpAppointment),
                resolveTypeCode(followUpAppointment),
                normalizeStatusCode(status),
                resolveDisplayStatus(status),
                resolveStatusColor(status),
                resolvePaymentStatusDisplay(followUpAppointment.getPaymentStatus()),
                followUpAppointment.getConsultationFee(),
                resolveFollowUpNote(followUpAppointment),
                resolveParentAppointmentId(followUpAppointment),
                isFollowUpAppointment(followUpAppointment)
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
        String normalizedType = foldText(resolveDisplayType(appointment));
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
        if (normalized.contains("chokham")
                || normalized.contains("chuakham")
                || normalized.contains("dangcho")
                || normalized.contains("pending")) {
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
            case COMPLETED -> "\u0110\u00e3 kh\u00e1m";
            case CANCELLED -> "H\u1ee7y l\u1ecbch";
            default -> "Ch\u01b0a kh\u00e1m";
        };
    }

    private String normalizeStatusCode(String storedStatus) {
        AppointmentStatusFilter filter = resolveStatusFilterFromStoredValue(storedStatus);
        return switch (filter) {
            case COMPLETED -> STATUS_COMPLETED;
            case CANCELLED -> STATUS_CANCELLED;
            default -> STATUS_PENDING;
        };
    }

    private String resolveStatusColor(String storedStatus) {
        AppointmentStatusFilter filter = resolveStatusFilterFromStoredValue(storedStatus);
        return switch (filter) {
            case COMPLETED -> "green";
            case CANCELLED -> "red";
            default -> "yellow";
        };
    }

    private String resolveDisplayType(Appointment appointment) {
        return resolveDisplayType(
                appointment == null ? null : appointment.getAppointmentType(),
                appointment == null ? null : resolveParentAppointmentId(appointment),
                appointment == null ? null : appointment.getFollowUpNote()
        );
    }

    private String resolveDisplayType(String storedType) {
        return resolveDisplayType(storedType, null, null);
    }

    private String resolveDisplayType(String storedType, Integer parentAppointmentId, String followUpNote) {
        String normalized = foldText(storedType);
        if (normalized != null && normalized.contains("taikham")) {
            return "T\u00e1i kh\u00e1m";
        }
        if (parentAppointmentId != null || trimToNull(followUpNote) != null) {
            return "T\u00e1i kh\u00e1m";
        }
        return "Kh\u00e1m b\u1ec7nh";
    }

    private String resolvePaymentStatusDisplay(String paymentStatus) {
        String normalized = paymentStatus == null ? null : paymentStatus.trim().toUpperCase(Locale.ROOT);
        if (normalized == null || normalized.isEmpty()) {
            return "Ch\u01b0a thanh to\u00e1n";
        }
        if (PAYMENT_STATUS_PAID.equals(normalized) || PAYMENT_STATUS_PAID_ONLINE.equals(normalized)) {
            return "\u0110\u00e3 thanh to\u00e1n";
        }
        if (normalized.contains("CANCEL")) {
            return "\u0110\u00e3 h\u1ee7y";
        }
        if (normalized.contains("FAIL")) {
            return "Thanh to\u00e1n th\u1ea5t b\u1ea1i";
        }
        return "Ch\u01b0a thanh to\u00e1n";
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

    private LocalDate parseFlexibleDate(String rawValue, String fieldName) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return null;
        }

        List<DateTimeFormatter> acceptedFormats = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-M-d")
        );

        for (DateTimeFormatter formatter : acceptedFormats) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }

        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // keep fallback below
        }

        throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "Dinh dang '" + fieldName + "' khong hop le. Ho tro: yyyy-MM-dd hoac dd/MM/yyyy."
        );
    }

    private LocalTime parseFlexibleTime(String rawValue, String fieldName) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return null;
        }

        String normalized = value
                .replace('.', ':')
                .replaceAll("(?i)\\bSA\\b", "AM")
                .replaceAll("(?i)\\bCH\\b", "PM")
                .replaceAll("(?i)\\bA\\.M\\.?\\b", "AM")
                .replaceAll("(?i)\\bP\\.M\\.?\\b", "PM")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);

        List<DateTimeFormatter> acceptedFormats = List.of(
                DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("H:mm:ss"),
                DateTimeFormatter.ofPattern("HH:mm:ss"),
                DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH)
        );

        for (DateTimeFormatter formatter : acceptedFormats) {
            try {
                return LocalTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }

        throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "Dinh dang '" + fieldName + "' khong hop le. Ho tro: HH:mm hoac hh:mm AM/PM."
        );
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
            case MONDAY -> "Thá»© 2";
            case TUESDAY -> "Thá»© 3";
            case WEDNESDAY -> "Thá»© 4";
            case THURSDAY -> "Thá»© 5";
            case FRIDAY -> "Thá»© 6";
            case SATURDAY -> "Thá»© 7";
            case SUNDAY -> "Chá»§ Nháº­t";
        };
    }

    private record PatientVisitStats(long newExamCount, long followUpCount, LocalDate latestVisitDate) {
        long totalVisitCount() {
            return newExamCount + followUpCount;
        }
    }

    private record FollowUpRequestPayload(LocalDate followUpDate, LocalTime followUpTime, String note) {
    }

    private record SlotRule(LocalDate date, LocalDateTime start, LocalDateTime end, String shift, int maxPatients) {
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


