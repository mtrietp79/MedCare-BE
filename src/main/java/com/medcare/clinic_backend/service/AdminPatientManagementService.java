package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.patient.*;
import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminPatientManagementService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final InvoiceRepository invoiceRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<AdminPatientListItemResponse> getPatients(String keyword,
                                                          String status,
                                                          int page,
                                                          int size,
                                                          String sort) {
        Boolean activeFilter = toActiveFilter(status);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), parseSort(sort));
        String keywordPattern = toLikePattern(trimToNull(keyword));
        Page<Patient> patients = keywordPattern == null
                ? patientRepository.findAdminPatients(activeFilter, pageable)
                : patientRepository.searchAdminPatientsByKeyword(keywordPattern, activeFilter, pageable);
        List<AdminPatientListItemResponse> content = patients.getContent().stream()
                .map(this::toListItemResponse)
                .toList();
        return new PageImpl<>(content, patients.getPageable(), patients.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminPatientDetailResponse getPatientDetail(Integer patientId) {
        Patient patient = findPatientOrThrow(patientId);
        long appointmentCount = appointmentRepository.countByPatientId(patientId);
        long completedAppointmentCount = appointmentRepository.countByPatientIdAndStatus(patientId, "COMPLETED");
        long cancelledAppointmentCount = appointmentRepository.countByPatientIdAndStatus(patientId, "CANCELLED");
        long medicalRecordCount = medicalRecordRepository.countByPatientId(patientId);
        long invoiceCount = invoiceRepository.countByMedicalRecordPatientId(patientId);
        long totalPaidAmount = safeLong(invoiceRepository.sumPaidAmountByPatientId(patientId));

        List<AdminPatientRecentAppointmentResponse> recentAppointments = appointmentRepository
                .findTop5ByPatientIdOrderByAppointmentDateDesc(patientId)
                .stream()
                .map(this::toRecentAppointment)
                .toList();

        List<AdminPatientRecentMedicalRecordResponse> recentMedicalRecords = medicalRecordRepository
                .findTop5ByPatientIdOrderByExaminationDateDesc(patientId)
                .stream()
                .map(this::toRecentMedicalRecord)
                .toList();

        return AdminPatientDetailResponse.builder()
                .id(patient.getId())
                .accountId(patient.getAccount() == null ? null : patient.getAccount().getId())
                .fullName(patient.getFullName())
                .email(patient.getEmail())
                .phone(patient.getPhone())
                .gender(patient.getGender())
                .dateOfBirth(patient.getDateOfBirth())
                .address(patient.getAddress())
                .avatar(patient.getAvatarUrl())
                .isActive(resolveIsActive(patient.getAccount()))
                .createdAt(resolveCreatedAt(patient.getAccount()))
                .statistics(new AdminPatientDetailStatisticsResponse(
                        appointmentCount,
                        completedAppointmentCount,
                        cancelledAppointmentCount,
                        medicalRecordCount,
                        invoiceCount,
                        totalPaidAmount
                ))
                .recentAppointments(recentAppointments)
                .recentMedicalRecords(recentMedicalRecords)
                .build();
    }

    @Transactional
    public AdminPatientLockStatusResponse lockPatient(Integer patientId) {
        Patient patient = findPatientWithAccountOrThrow(patientId);
        Account account = requirePatientAccount(patient);
        account.setIsActive(false);
        accountRepository.saveAndFlush(account);
        return new AdminPatientLockStatusResponse(
                "Khóa tài khoản bệnh nhân thành công",
                patient.getId(),
                account.getId(),
                false
        );
    }

    @Transactional
    public AdminPatientLockStatusResponse unlockPatient(Integer patientId) {
        Patient patient = findPatientWithAccountOrThrow(patientId);
        Account account = requirePatientAccount(patient);
        account.setIsActive(true);
        accountRepository.saveAndFlush(account);
        return new AdminPatientLockStatusResponse(
                "Mở khóa tài khoản bệnh nhân thành công",
                patient.getId(),
                account.getId(),
                true
        );
    }

    @Transactional
    public Map<String, Object> resetPassword(Integer patientId, String temporaryPassword) {
        Account account = getPatientAccount(patientId);
        String tempPassword = trimToNull(temporaryPassword);
        if (tempPassword == null) {
            tempPassword = generateTemporaryPassword();
        }
        account.setPassword(passwordEncoder.encode(tempPassword));
        account.setMustChangePassword(true);
        account.clearPasswordRecoveryState();
        accountRepository.save(account);
        return Map.of(
                "message", "Reset mật khẩu bệnh nhân thành công",
                "temporaryPassword", tempPassword,
                "mustChangePassword", true
        );
    }

    @Transactional(readOnly = true)
    public AdminPatientStatsResponse getStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime to = from.plusMonths(1);
        return new AdminPatientStatsResponse(
                patientRepository.count(),
                patientRepository.countActivePatients(),
                patientRepository.countLockedPatients(),
                patientRepository.countNewPatientsBetween(from, to)
        );
    }

    private AdminPatientListItemResponse toListItemResponse(Patient patient) {
        Integer patientId = patient.getId();
        return AdminPatientListItemResponse.builder()
                .id(patientId)
                .accountId(patient.getAccount() == null ? null : patient.getAccount().getId())
                .fullName(patient.getFullName())
                .email(patient.getEmail())
                .phone(patient.getPhone())
                .gender(patient.getGender())
                .dateOfBirth(patient.getDateOfBirth())
                .address(patient.getAddress())
                .avatar(patient.getAvatarUrl())
                .isActive(resolveIsActive(patient.getAccount()))
                .createdAt(resolveCreatedAt(patient.getAccount()))
                .appointmentCount(appointmentRepository.countByPatientId(patientId))
                .medicalRecordCount(medicalRecordRepository.countByPatientId(patientId))
                .invoiceCount(invoiceRepository.countByMedicalRecordPatientId(patientId))
                .build();
    }

    private AdminPatientRecentAppointmentResponse toRecentAppointment(Appointment appointment) {
        LocalDateTime dateTime = appointment.getAppointmentDate();
        return AdminPatientRecentAppointmentResponse.builder()
                .id(appointment.getId())
                .appointmentCode(appointment.getAppointmentCode())
                .doctorName(appointment.getDoctor() == null ? null : appointment.getDoctor().getFullName())
                .appointmentDate(dateTime == null ? null : dateTime.toLocalDate())
                .appointmentTime(dateTime == null ? null : LocalTime.from(dateTime))
                .appointmentType(appointment.getAppointmentTypeDisplay())
                .status(appointment.getStatusDisplay())
                .build();
    }

    private AdminPatientRecentMedicalRecordResponse toRecentMedicalRecord(MedicalRecord record) {
        return AdminPatientRecentMedicalRecordResponse.builder()
                .id(record.getId())
                .appointmentCode(record.getAppointment() == null ? null : record.getAppointment().getAppointmentCode())
                .doctorName(record.getDoctor() == null ? null : record.getDoctor().getFullName())
                .diagnosis(record.getDiagnosis())
                .examDate(record.getExaminationDate())
                .build();
    }

    private Patient findPatientOrThrow(Integer patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh nhân."));
    }

    private Patient findPatientWithAccountOrThrow(Integer patientId) {
        return patientRepository.findByIdWithAccount(patientId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy bệnh nhân."));
    }

    private Account getPatientAccount(Integer patientId) {
        return requirePatientAccount(findPatientWithAccountOrThrow(patientId));
    }

    private Account requirePatientAccount(Patient patient) {
        Account account = patient.getAccount();
        if (account == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản bệnh nhân.");
        }
        return account;
    }

    private Boolean toActiveFilter(String status) {
        AdminPatientStatusFilter filter = parseStatus(status);
        if (filter == AdminPatientStatusFilter.ALL) {
            return null;
        }
        return filter == AdminPatientStatusFilter.ACTIVE;
    }

    private AdminPatientStatusFilter parseStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return AdminPatientStatusFilter.ALL;
        }
        if ("ACTIVE".equalsIgnoreCase(normalized)) {
            return AdminPatientStatusFilter.ACTIVE;
        }
        if ("LOCKED".equalsIgnoreCase(normalized)
                || "INACTIVE".equalsIgnoreCase(normalized)
                || "DEACTIVATED".equalsIgnoreCase(normalized)) {
            return AdminPatientStatusFilter.LOCKED;
        }
        return AdminPatientStatusFilter.ALL;
    }

    private Sort parseSort(String sort) {
        Sort defaultSort = Sort.by(Sort.Direction.DESC, "account.createdAt");
        if (sort == null || sort.isBlank()) {
            return defaultSort;
        }
        String normalizedSort = sort.trim().toLowerCase();
        if ("newest".equals(normalizedSort)) {
            return defaultSort;
        }
        if ("oldest".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.ASC, "account.createdAt");
        }
        if ("name_asc".equals(normalizedSort) || "name-asc".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.ASC, "fullName");
        }
        if ("name_desc".equals(normalizedSort) || "name-desc".equals(normalizedSort)) {
            return Sort.by(Sort.Direction.DESC, "fullName");
        }
        return defaultSort;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toLikePattern(String keyword) {
        return keyword == null ? null : "%" + keyword + "%";
    }

    private long safeLong(Double value) {
        if (value == null) {
            return 0L;
        }
        return Math.round(value);
    }

    private Boolean resolveIsActive(Account account) {
        if (account == null) {
            return false;
        }
        return Boolean.TRUE.equals(account.getIsActive());
    }

    private LocalDateTime resolveCreatedAt(Account account) {
        return account == null ? null : account.getCreatedAt();
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            builder.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return builder.toString();
    }
}