package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.cancellation.AdminCancellationActionRequest;
import com.medcare.clinic_backend.dto.cancellation.AdminCancellationActionResponse;
import com.medcare.clinic_backend.dto.cancellation.AdminCancellationRequestDetailResponse;
import com.medcare.clinic_backend.dto.cancellation.AdminCancellationRequestListItemResponse;
import com.medcare.clinic_backend.dto.cancellation.AdminCancellationRequestStatsResponse;
import com.medcare.clinic_backend.dto.cancellation.CreateCancellationRequestDto;
import com.medcare.clinic_backend.dto.cancellation.CreateCancellationRequestResponse;
import com.medcare.clinic_backend.dto.cancellation.PatientCancellationRequestSummary;
import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.AppointmentCancellationRequest;
import com.medcare.clinic_backend.entity.AppointmentCancellationRequestStatus;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.entity.TransactionLog;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AccountRepository;
import com.medcare.clinic_backend.repository.AppointmentCancellationRequestRepository;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AppointmentCancellationService {

    private static final String APPOINTMENT_STATUS_CANCEL_REQUESTED = "CANCEL_REQUESTED";
    private static final String APPOINTMENT_STATUS_CANCELLED = "CANCELLED";
    private static final String APPOINTMENT_STATUS_CANCEL_REJECTED = "CANCEL_REJECTED";
    private static final String APPOINTMENT_STATUS_CONFIRMED = "CONFIRMED";
    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String PAYMENT_STATUS_PAID_ONLINE = "PAID_ONLINE";
    private static final String PAYMENT_STATUS_REFUND_PENDING = "REFUND_PENDING";
    private static final String PAYMENT_STATUS_REFUNDED = "REFUNDED";
    private static final String VNPAY_SUCCESS_CODE = "00";
    private static final String MANUAL_PAID_CODE = "MANUAL_PAID";

    private static final Set<String> ACTIVE_REQUEST_STATUSES = Set.of(
            AppointmentCancellationRequestStatus.PENDING,
            AppointmentCancellationRequestStatus.APPROVED,
            AppointmentCancellationRequestStatus.REFUNDED
    );

    @Autowired
    private AppointmentCancellationRequestRepository cancellationRequestRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Transactional
    public CreateCancellationRequestResponse createCancellationRequest(Integer appointmentId,
                                                                       Integer patientId,
                                                                       CreateCancellationRequestDto request) {
        validateCreateRequest(request);

        Appointment appointment = appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n h\u1ee7y l\u1ecbch h\u1eb9n n\u00e0y."));

        validateAppointmentCanRequestCancellation(appointment);

        if (cancellationRequestRepository.existsByAppointmentIdAndStatusIn(appointment.getId(), ACTIVE_REQUEST_STATUSES)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "L\u1ecbch h\u1eb9n \u0111\u00e3 c\u00f3 y\u00eau c\u1ea7u h\u1ee7y \u0111ang \u0111\u01b0\u1ee3c x\u1eed l\u00fd.");
        }

        boolean paid = isPaidAppointment(appointment);
        if (paid) {
            validateBankInfoWhenPaid(request);
        }

        LocalDateTime now = LocalDateTime.now();
        AppointmentCancellationRequest cancellationRequest = new AppointmentCancellationRequest();
        cancellationRequest.setAppointment(appointment);
        cancellationRequest.setPatient(appointment.getPatient());
        cancellationRequest.setInvoice(resolveInvoiceForAppointment(appointment.getId()).orElse(null));
        cancellationRequest.setCancelReason(trimToNull(request.getCancelReason()));
        cancellationRequest.setBankName(trimToNull(request.getBankName()));
        cancellationRequest.setBankAccountNumber(trimToNull(request.getBankAccountNumber()));
        cancellationRequest.setBankAccountHolder(trimToNull(request.getBankAccountHolder()));
        cancellationRequest.setPatientNote(trimToNull(request.getPatientNote()));
        cancellationRequest.setRefundAmount(resolveRefundAmount(appointment));
        cancellationRequest.setStatus(AppointmentCancellationRequestStatus.PENDING);
        cancellationRequest.setCreatedAt(now);
        cancellationRequest.setUpdatedAt(now);

        AppointmentCancellationRequest saved = cancellationRequestRepository.save(cancellationRequest);

        appointment.setStatus(APPOINTMENT_STATUS_CANCEL_REQUESTED);
        if (paid) {
            appointment.setPaymentStatus(PAYMENT_STATUS_REFUND_PENDING);
        }
        appointmentRepository.save(appointment);

        return new CreateCancellationRequestResponse(
                "G\u1eedi y\u00eau c\u1ea7u h\u1ee7y l\u1ecbch th\u00e0nh c\u00f4ng. Admin s\u1ebd ki\u1ec3m tra v\u00e0 x\u1eed l\u00fd trong th\u1eddi gian s\u1edbm nh\u1ea5t.",
                saved.getId(),
                appointment.getId(),
                saved.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public List<PatientCancellationRequestSummary> getPatientCancellationRequests(Integer patientId) {
        return cancellationRequestRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::toPatientSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Integer, PatientCancellationRequestSummary> getLatestCancellationSummariesByAppointmentIds(List<Integer> appointmentIds) {
        if (appointmentIds == null || appointmentIds.isEmpty()) {
            return Map.of();
        }
        List<AppointmentCancellationRequest> requests = cancellationRequestRepository
                .findByAppointmentIdInOrderByCreatedAtDesc(appointmentIds);
        Map<Integer, PatientCancellationRequestSummary> result = new HashMap<>();
        for (AppointmentCancellationRequest request : requests) {
            if (request.getAppointment() == null || request.getAppointment().getId() == null) {
                continue;
            }
            result.putIfAbsent(request.getAppointment().getId(), toPatientSummary(request));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Page<AdminCancellationRequestListItemResponse> getAdminList(String keyword,
                                                                       String status,
                                                                       int page,
                                                                       int size,
                                                                       String sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), parseSort(sort));
        String statusFilter = parseStatusFilter(status);
        String keywordPattern = toLikePattern(trimToNull(keyword));
        Page<AppointmentCancellationRequest> items = cancellationRequestRepository.findAdminList(statusFilter, keywordPattern, pageable);
        List<AdminCancellationRequestListItemResponse> content = items.getContent().stream()
                .map(this::toAdminListItem)
                .toList();
        return new PageImpl<>(content, items.getPageable(), items.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminCancellationRequestDetailResponse getAdminDetail(Integer id) {
        AppointmentCancellationRequest request = cancellationRequestRepository.findDetailedById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Kh\u00f4ng t\u00ecm th\u1ea5y y\u00eau c\u1ea7u h\u1ee7y."));
        return toAdminDetail(request);
    }

    @Transactional(readOnly = true)
    public AdminCancellationRequestStatsResponse getAdminStats() {
        long total = cancellationRequestRepository.count();
        long pending = cancellationRequestRepository.countByStatus(AppointmentCancellationRequestStatus.PENDING);
        long approved = cancellationRequestRepository.countByStatus(AppointmentCancellationRequestStatus.APPROVED);
        long rejected = cancellationRequestRepository.countByStatus(AppointmentCancellationRequestStatus.REJECTED);
        long refunded = cancellationRequestRepository.countByStatus(AppointmentCancellationRequestStatus.REFUNDED);
        double pendingAmount = safeDouble(cancellationRequestRepository.sumRefundAmountByStatus(AppointmentCancellationRequestStatus.PENDING));
        double approvedAmount = safeDouble(cancellationRequestRepository.sumRefundAmountByStatus(AppointmentCancellationRequestStatus.APPROVED));
        double refundedAmount = safeDouble(cancellationRequestRepository.sumRefundAmountByStatus(AppointmentCancellationRequestStatus.REFUNDED));
        return AdminCancellationRequestStatsResponse.builder()
                .total(total)
                .pending(pending)
                .approved(approved)
                .rejected(rejected)
                .refunded(refunded)
                .totalRefundAmountPending(pendingAmount + approvedAmount)
                .totalRefundAmountProcessed(refundedAmount)
                .build();
    }

    @Transactional
    public AdminCancellationActionResponse approve(Integer id, String adminUsername, AdminCancellationActionRequest request) {
        AppointmentCancellationRequest cancellationRequest = findForAdminAction(id);
        if (!AppointmentCancellationRequestStatus.PENDING.equals(cancellationRequest.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ch\u1ec9 c\u00f3 th\u1ec3 duy\u1ec7t y\u00eau c\u1ea7u \u0111ang ch\u1edd x\u1eed l\u00fd.");
        }
        Account admin = resolveAdminAccount(adminUsername);
        cancellationRequest.setStatus(AppointmentCancellationRequestStatus.APPROVED);
        cancellationRequest.setAdminNote(trimToNull(request == null ? null : request.getAdminNote()));
        cancellationRequest.setProcessedByAdmin(admin);
        cancellationRequest.setProcessedAt(LocalDateTime.now());
        cancellationRequestRepository.save(cancellationRequest);

        Appointment appointment = cancellationRequest.getAppointment();
        appointment.setStatus(APPOINTMENT_STATUS_CANCELLED);
        if (isPaidAppointment(appointment) || safeDouble(cancellationRequest.getRefundAmount()) > 0) {
            appointment.setPaymentStatus(PAYMENT_STATUS_REFUND_PENDING);
        } else {
            appointment.setPaymentStatus("CANCELLED");
        }
        appointmentRepository.save(appointment);

        return new AdminCancellationActionResponse("Duy\u1ec7t y\u00eau c\u1ea7u h\u1ee7y th\u00e0nh c\u00f4ng", AppointmentCancellationRequestStatus.APPROVED);
    }

    @Transactional
    public AdminCancellationActionResponse reject(Integer id, String adminUsername, AdminCancellationActionRequest request) {
        AppointmentCancellationRequest cancellationRequest = findForAdminAction(id);
        if (!AppointmentCancellationRequestStatus.PENDING.equals(cancellationRequest.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ch\u1ec9 c\u00f3 th\u1ec3 t\u1eeb ch\u1ed1i y\u00eau c\u1ea7u \u0111ang ch\u1edd x\u1eed l\u00fd.");
        }
        Account admin = resolveAdminAccount(adminUsername);
        cancellationRequest.setStatus(AppointmentCancellationRequestStatus.REJECTED);
        cancellationRequest.setAdminNote(trimToNull(request == null ? null : request.getAdminNote()));
        cancellationRequest.setProcessedByAdmin(admin);
        cancellationRequest.setProcessedAt(LocalDateTime.now());
        cancellationRequestRepository.save(cancellationRequest);

        Appointment appointment = cancellationRequest.getAppointment();
        boolean requiresRefund = safeDouble(cancellationRequest.getRefundAmount()) > 0;
        appointment.setStatus(APPOINTMENT_STATUS_CANCEL_REJECTED);
        if (requiresRefund) {
            appointment.setPaymentStatus(resolvePaidPaymentStatus(appointment));
        } else {
            appointment.setPaymentStatus("UNPAID");
        }
        appointmentRepository.save(appointment);

        return new AdminCancellationActionResponse("T\u1eeb ch\u1ed1i y\u00eau c\u1ea7u h\u1ee7y th\u00e0nh c\u00f4ng", AppointmentCancellationRequestStatus.REJECTED);
    }

    @Transactional
    public AdminCancellationActionResponse markRefunded(Integer id, String adminUsername, AdminCancellationActionRequest request) {
        AppointmentCancellationRequest cancellationRequest = findForAdminAction(id);
        if (!AppointmentCancellationRequestStatus.APPROVED.equals(cancellationRequest.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ch\u1ec9 c\u00f3 th\u1ec3 \u0111\u00e1nh d\u1ea5u \u0111\u00e3 ho\u00e0n ti\u1ec1n khi y\u00eau c\u1ea7u \u0111\u00e3 \u0111\u01b0\u1ee3c duy\u1ec7t.");
        }
        Account admin = resolveAdminAccount(adminUsername);
        cancellationRequest.setStatus(AppointmentCancellationRequestStatus.REFUNDED);
        cancellationRequest.setAdminNote(trimToNull(request == null ? null : request.getAdminNote()));
        cancellationRequest.setProcessedByAdmin(admin);
        cancellationRequest.setProcessedAt(LocalDateTime.now());
        cancellationRequestRepository.save(cancellationRequest);

        Appointment appointment = cancellationRequest.getAppointment();
        appointment.setStatus(APPOINTMENT_STATUS_CANCELLED);
        appointment.setPaymentStatus(PAYMENT_STATUS_REFUNDED);
        appointmentRepository.save(appointment);

        return new AdminCancellationActionResponse("\u0110\u00e3 \u0111\u00e1nh d\u1ea5u x\u1eed l\u00fd ho\u00e0n ti\u1ec1n", AppointmentCancellationRequestStatus.REFUNDED);
    }

    private AppointmentCancellationRequest findForAdminAction(Integer id) {
        return cancellationRequestRepository.findDetailedById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Kh\u00f4ng t\u00ecm th\u1ea5y y\u00eau c\u1ea7u h\u1ee7y."));
    }

    private void validateCreateRequest(CreateCancellationRequestDto request) {
        if (request == null || trimToNull(request.getCancelReason()) == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui l\u00f2ng nh\u1eadp l\u00fd do h\u1ee7y.");
        }
    }

    private void validateBankInfoWhenPaid(CreateCancellationRequestDto request) {
        if (trimToNull(request.getBankName()) == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui l\u00f2ng nh\u1eadp t\u00ean ng\u00e2n h\u00e0ng.");
        }
        if (trimToNull(request.getBankAccountNumber()) == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui l\u00f2ng nh\u1eadp s\u1ed1 t\u00e0i kho\u1ea3n.");
        }
        if (trimToNull(request.getBankAccountHolder()) == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui l\u00f2ng nh\u1eadp t\u00ean ch\u1ee7 t\u00e0i kho\u1ea3n.");
        }
    }

    private void validateAppointmentCanRequestCancellation(Appointment appointment) {
        String status = normalizeAppointmentStatus(appointment.getStatus());
        if ("COMPLETED".equals(status)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Kh\u00f4ng th\u1ec3 h\u1ee7y l\u1ecbch \u0111\u00e3 kh\u00e1m.");
        }
        if ("CANCELLED".equals(status) || APPOINTMENT_STATUS_CANCEL_REQUESTED.equals(status)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "L\u1ecbch h\u1eb9n kh\u00f4ng th\u1ec3 g\u1eedi y\u00eau c\u1ea7u h\u1ee7y \u1edf tr\u1ea1ng th\u00e1i hi\u1ec7n t\u1ea1i.");
        }
    }

    private boolean isPaidAppointment(Appointment appointment) {
        String paymentStatus = normalizePaymentStatus(appointment.getPaymentStatus());
        return PAYMENT_STATUS_PAID.equals(paymentStatus) || PAYMENT_STATUS_PAID_ONLINE.equals(paymentStatus);
    }

    private String resolvePaidPaymentStatus(Appointment appointment) {
        String current = appointment.getPaymentStatus();
        if (current != null && PAYMENT_STATUS_PAID_ONLINE.equalsIgnoreCase(current.trim())) {
            return PAYMENT_STATUS_PAID_ONLINE;
        }
        return PAYMENT_STATUS_PAID;
    }

    private double resolveRefundAmount(Appointment appointment) {
        if (appointment == null || appointment.getId() == null || !isPaidAppointment(appointment)) {
            return 0.0;
        }
        TransactionLog paidLog = transactionLogRepository
                .findTopByAppointmentIdAndResponseCodeOrderByCreatedAtDesc(appointment.getId(), VNPAY_SUCCESS_CODE);
        if (paidLog == null) {
            paidLog = transactionLogRepository
                    .findTopByAppointmentIdAndResponseCodeOrderByCreatedAtDesc(appointment.getId(), MANUAL_PAID_CODE);
        }
        if (paidLog != null && paidLog.getAmount() != null && paidLog.getAmount() > 0) {
            return paidLog.getAmount();
        }
        return appointment.getConsultationFee() == null ? 0.0 : appointment.getConsultationFee();
    }

    private java.util.Optional<Invoice> resolveInvoiceForAppointment(Integer appointmentId) {
        if (appointmentId == null) {
            return java.util.Optional.empty();
        }
        return invoiceRepository.findFirstByAppointment_IdOrderByCreatedAtDesc(appointmentId);
    }

    private Account resolveAdminAccount(String adminUsername) {
        return accountRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Kh\u00f4ng t\u00ecm th\u1ea5y t\u00e0i kho\u1ea3n admin \u0111\u0103ng nh\u1eadp."));
    }

    private PatientCancellationRequestSummary toPatientSummary(AppointmentCancellationRequest request) {
        return PatientCancellationRequestSummary.builder()
                .id(request.getId())
                .status(request.getStatus())
                .statusLabel(AppointmentCancellationRequestStatus.toLabel(request.getStatus()))
                .refundAmount(request.getRefundAmount())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private AdminCancellationRequestListItemResponse toAdminListItem(AppointmentCancellationRequest request) {
        Appointment appointment = request.getAppointment();
        Patient patient = request.getPatient();
        LocalDateTime appointmentDateTime = appointment == null ? null : appointment.getAppointmentDate();
        return AdminCancellationRequestListItemResponse.builder()
                .id(request.getId())
                .appointmentId(appointment == null ? null : appointment.getId())
                .appointmentCode(appointment == null ? null : appointment.getAppointmentCode())
                .patientName(patient == null ? null : patient.getFullName())
                .patientEmail(patient == null ? null : patient.getEmail())
                .doctorName(appointment == null ? null : appointment.getDoctorName())
                .appointmentDate(appointmentDateTime == null ? null : appointmentDateTime.toLocalDate())
                .appointmentTime(appointmentDateTime == null ? null : appointmentDateTime.toLocalTime())
                .refundAmount(request.getRefundAmount())
                .cancelReason(request.getCancelReason())
                .patientNote(request.getPatientNote())
                .bankName(request.getBankName())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankAccountHolder(request.getBankAccountHolder())
                .status(request.getStatus())
                .statusLabel(AppointmentCancellationRequestStatus.toLabel(request.getStatus()))
                .adminNote(request.getAdminNote())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private AdminCancellationRequestDetailResponse toAdminDetail(AppointmentCancellationRequest request) {
        Appointment appointment = request.getAppointment();
        Patient patient = request.getPatient();
        Account processedBy = request.getProcessedByAdmin();
        LocalDateTime appointmentDateTime = appointment == null ? null : appointment.getAppointmentDate();
        String appointmentStatus = appointment == null ? null : normalizeAppointmentStatus(appointment.getStatus());
        String paymentStatus = appointment == null ? null : normalizePaymentStatus(appointment.getPaymentStatus());
        return AdminCancellationRequestDetailResponse.builder()
                .id(request.getId())
                .appointmentId(appointment == null ? null : appointment.getId())
                .appointmentCode(appointment == null ? null : appointment.getAppointmentCode())
                .appointmentStatus(appointmentStatus)
                .appointmentStatusLabel(resolveAppointmentStatusLabel(appointmentStatus))
                .paymentStatus(paymentStatus)
                .paymentStatusLabel(resolvePaymentStatusLabel(paymentStatus))
                .patientId(patient == null ? null : patient.getId())
                .patientName(patient == null ? null : patient.getFullName())
                .patientEmail(patient == null ? null : patient.getEmail())
                .patientPhone(patient == null ? null : patient.getPhone())
                .doctorName(appointment == null ? null : appointment.getDoctorName())
                .appointmentDate(appointmentDateTime == null ? null : appointmentDateTime.toLocalDate())
                .appointmentTime(appointmentDateTime == null ? null : appointmentDateTime.toLocalTime())
                .invoiceId(request.getInvoice() == null ? null : request.getInvoice().getId())
                .refundAmount(request.getRefundAmount())
                .cancelReason(request.getCancelReason())
                .bankName(request.getBankName())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankAccountHolder(request.getBankAccountHolder())
                .patientNote(request.getPatientNote())
                .status(request.getStatus())
                .statusLabel(AppointmentCancellationRequestStatus.toLabel(request.getStatus()))
                .adminNote(request.getAdminNote())
                .processedByAdminId(processedBy == null ? null : processedBy.getId())
                .processedByAdminUsername(processedBy == null ? null : processedBy.getUsername())
                .processedAt(request.getProcessedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    private Sort parseSort(String sort) {
        String normalized = trimToNull(sort);
        if (normalized == null || "newest".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("oldest".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }
        if ("refund_desc".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Direction.DESC, "refundAmount");
        }
        if ("refund_asc".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Direction.ASC, "refundAmount");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private String parseStatusFilter(String status) {
        String normalized = trimToNull(status);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case AppointmentCancellationRequestStatus.PENDING,
                    AppointmentCancellationRequestStatus.APPROVED,
                    AppointmentCancellationRequestStatus.REJECTED,
                    AppointmentCancellationRequestStatus.REFUNDED -> normalized.toUpperCase(Locale.ROOT);
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "Tr\u1ea1ng th\u00e1i l\u1ecdc kh\u00f4ng h\u1ee3p l\u1ec7.");
        };
    }

    private String normalizeAppointmentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }
        String upper = status.trim().toUpperCase(Locale.ROOT);
        if (upper.contains("CANCEL_REQUEST")) {
            return APPOINTMENT_STATUS_CANCEL_REQUESTED;
        }
        if (upper.contains("CANCEL_REJECT")) {
            return APPOINTMENT_STATUS_CANCEL_REJECTED;
        }
        if (upper.contains("CANCEL")) {
            return APPOINTMENT_STATUS_CANCELLED;
        }
        if (upper.contains("COMPLETED")) {
            return "COMPLETED";
        }
        if (upper.contains("CONFIRMED")) {
            return APPOINTMENT_STATUS_CONFIRMED;
        }
        if (upper.contains("PENDING_PAYMENT")) {
            return "PENDING_PAYMENT";
        }
        return upper;
    }

    private String normalizePaymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNPAID";
        }
        String upper = status.trim().toUpperCase(Locale.ROOT);
        if ("PAID_ONLINE".equals(upper)) {
            return PAYMENT_STATUS_PAID_ONLINE;
        }
        if ("PAID".equals(upper)) {
            return PAYMENT_STATUS_PAID;
        }
        if (upper.contains("REFUND_PENDING")) {
            return PAYMENT_STATUS_REFUND_PENDING;
        }
        if (upper.contains("REFUNDED")) {
            return PAYMENT_STATUS_REFUNDED;
        }
        if (upper.contains("CANCEL")) {
            return "CANCELLED";
        }
        if (upper.contains("FAIL")) {
            return "FAILED";
        }
        if (upper.contains("PENDING")) {
            return "PENDING";
        }
        return "UNPAID";
    }

    public static String resolveAppointmentStatusLabel(String status) {
        String normalized = status == null ? "PENDING" : status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case APPOINTMENT_STATUS_CANCEL_REQUESTED -> "\u0110\u00e3 h\u1ee7y - ch\u1edd x\u00e1c nh\u1eadn";
            case APPOINTMENT_STATUS_CANCEL_REJECTED -> "T\u1eeb ch\u1ed1i h\u1ee7y";
            case APPOINTMENT_STATUS_CANCELLED -> "\u0110\u00e3 h\u1ee7y";
            case "COMPLETED" -> "\u0110\u00e3 kh\u00e1m";
            case APPOINTMENT_STATUS_CONFIRMED -> "Ch\u1edd kh\u00e1m";
            case "PENDING_PAYMENT" -> "Ch\u1edd thanh to\u00e1n";
            default -> "Ch\u01b0a kh\u00e1m";
        };
    }

    public static String resolvePaymentStatusLabel(String status) {
        return resolvePaymentStatusLabel(status, null);
    }

    public static String resolvePaymentStatusLabel(String status, String appointmentStatus) {
        String appointmentCode = appointmentStatus == null ? null : appointmentStatus.trim().toUpperCase(Locale.ROOT);
        if (APPOINTMENT_STATUS_CANCEL_REQUESTED.equals(appointmentCode)) {
            return "\u0110\u00e3 h\u1ee7y - ch\u1edd x\u00e1c nh\u1eadn";
        }
        if (APPOINTMENT_STATUS_CANCEL_REJECTED.equals(appointmentCode)) {
            return "T\u1eeb ch\u1ed1i h\u1ee7y";
        }
        if (APPOINTMENT_STATUS_CANCELLED.equals(appointmentCode)) {
            return "\u0110\u00e3 h\u1ee7y";
        }
        String normalized = status == null ? "UNPAID" : status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case PAYMENT_STATUS_PAID, PAYMENT_STATUS_PAID_ONLINE -> "\u0110\u00e3 thanh to\u00e1n";
            case PAYMENT_STATUS_REFUND_PENDING -> "Ch\u1edd ho\u00e0n ti\u1ec1n";
            case PAYMENT_STATUS_REFUNDED -> "\u0110\u00e3 ho\u00e0n ti\u1ec1n";
            case "FAILED" -> "Thanh to\u00e1n th\u1ea5t b\u1ea1i";
            case "CANCELLED" -> "\u0110\u00e3 h\u1ee7y";
            case "PENDING" -> "\u0110ang ch\u1edd thanh to\u00e1n";
            default -> "Ch\u01b0a thanh to\u00e1n";
        };
    }

    public static boolean isCancelledAppointmentStatus(String status) {
        return com.medcare.clinic_backend.util.FinanceInvoiceRules.isCancelledAppointmentStatus(status);
    }

    private String toLikePattern(String keyword) {
        if (keyword == null) {
            return null;
        }
        return "%" + keyword.toLowerCase(Locale.ROOT) + "%";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }
}
