package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.cancellation.PatientCancellationRequestSummary;
import com.medcare.clinic_backend.dto.invoice.FinanceSummaryResponse;
import com.medcare.clinic_backend.dto.invoice.InvoiceResponse;
import com.medcare.clinic_backend.dto.invoice.PatientInvoiceDetailResponse;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.entity.ServicePackageBooking;
import com.medcare.clinic_backend.entity.TransactionLog;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.MedicalRecordRepository;
import com.medcare.clinic_backend.repository.PrescriptionDetailRepository;
import com.medcare.clinic_backend.repository.ServiceDetailRepository;
import com.medcare.clinic_backend.repository.ServicePackageBookingRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
import com.medcare.clinic_backend.util.AppointmentTypeCatalog;
import com.medcare.clinic_backend.util.AppointmentTypeCatalog.ResolvedType;
import com.medcare.clinic_backend.util.FinanceInvoiceRules;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Service
@Transactional(readOnly = true)
public class FinanceService {

    private static final String SOURCE_APPOINTMENT = "APPOINTMENT";
    private static final String SOURCE_INVOICE = "INVOICE";
    private static final String SOURCE_MEDICAL_RECORD = "MEDICAL_RECORD";
    private static final String SOURCE_SERVICE_PACKAGE = "SERVICE_PACKAGE";

    private static final String CATEGORY_APPOINTMENT_BOOKING = "APPOINTMENT_BOOKING";
    private static final String CATEGORY_POST_EXAM = "POST_EXAM";
    private static final String CATEGORY_FOLLOW_UP = "FOLLOW_UP";
    private static final String CATEGORY_SERVICE_PACKAGE = "SERVICE_PACKAGE";

    private static final String SUCCESS_CODE = "00";
    private static final String MANUAL_PAID_CODE = "MANUAL_PAID";

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ServicePackageBookingRepository servicePackageBookingRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private PrescriptionDetailRepository prescriptionDetailRepository;

    @Autowired
    private ServiceDetailRepository serviceDetailRepository;

    @Autowired
    private FinanceStatsService financeStatsService;

    @Autowired
    private AppointmentCancellationService appointmentCancellationService;

    public List<InvoiceResponse> getInvoiceResponsesForAdmin(String keyword, String status) {
        return getInvoiceResponsesForAdmin(keyword, status, null);
    }

    public List<InvoiceResponse> getInvoiceResponsesForAdmin(String keyword, String status, String category) {
        List<InvoiceResponse> responses = new ArrayList<>();
        responses.addAll(toAppointmentResponses(appointmentRepository.findAll()));
        responses.addAll(toInvoiceResponses(invoiceRepository.findAll()));
        responses.addAll(toServicePackageResponses(servicePackageBookingRepository.findAll()));
        return filterInvoices(responses, keyword, status, category);
    }

    public List<InvoiceResponse> getInvoiceResponsesForDoctor(Integer doctorId, String keyword, String status) {
        return getInvoiceResponsesForDoctor(doctorId, keyword, status, null);
    }

    public List<InvoiceResponse> getInvoiceResponsesForDoctor(Integer doctorId, String keyword, String status, String category) {
        List<InvoiceResponse> responses = new ArrayList<>();
        responses.addAll(toAppointmentResponses(appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctorId)));
        responses.addAll(toInvoiceResponses(invoiceRepository.findByMedicalRecordDoctorIdOrderByCreatedAtDesc(doctorId)));
        return filterInvoices(responses, keyword, status, category);
    }

    public List<InvoiceResponse> getInvoiceResponsesForPatient(Integer patientId, String keyword, String status) {
        return getInvoiceResponsesForPatient(patientId, keyword, status, null, null);
    }

    public List<InvoiceResponse> getInvoiceResponsesForPatient(Integer patientId, String keyword, String status, String category) {
        return getInvoiceResponsesForPatient(patientId, keyword, status, category, null);
    }

    public List<InvoiceResponse> getInvoiceResponsesForPatient(Integer patientId,
                                                               String keyword,
                                                               String status,
                                                               String category,
                                                               String type) {
        List<InvoiceResponse> responses = new ArrayList<>();
        responses.addAll(toAppointmentResponses(appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(patientId)));
        responses.addAll(toInvoiceResponses(invoiceRepository.findByMedicalRecordPatientIdOrderByCreatedAtDesc(patientId)));
        responses.addAll(toServicePackageResponses(servicePackageBookingRepository.findByPatientIdOrderByCreatedAtDesc(patientId)));
        return filterInvoices(responses, keyword, status, category, type);
    }

    public InvoiceResponse getInvoiceResponseByRecordId(Integer recordId, Integer doctorIdOrNull) {
        Invoice invoice = (doctorIdOrNull == null)
                ? invoiceRepository.findByMedicalRecordId(recordId).orElse(null)
                : invoiceRepository.findByMedicalRecordIdAndMedicalRecordDoctorId(recordId, doctorIdOrNull).orElse(null);
        return invoice == null ? null : toInvoiceResponse(invoice, resolvePaidInvoiceLog(invoice.getId()), null);
    }

    public InvoiceResponse getInvoiceResponseByRecordIdForPatient(Integer recordId, Integer patientId) {
        Invoice invoice = invoiceRepository.findByMedicalRecordIdAndMedicalRecordPatientId(recordId, patientId).orElse(null);
        return invoice == null ? null : toInvoiceResponse(invoice, resolvePaidInvoiceLog(invoice.getId()), null);
    }

    public InvoiceResponse getInvoiceResponseByIdForPatient(Integer invoiceId, Integer patientId) {
        Invoice invoice = invoiceRepository.findByIdAndMedicalRecordPatientId(invoiceId, patientId).orElse(null);
        return invoice == null ? null : toInvoiceResponse(invoice, resolvePaidInvoiceLog(invoice.getId()), null);
    }

    public PatientInvoiceDetailResponse getPatientInvoiceDetail(Integer patientId,
                                                                Integer id,
                                                                String sourceType,
                                                                String uniqueKey) {
        if (uniqueKey != null && !uniqueKey.isBlank()) {
            PatientInvoiceDetailResponse detail = resolvePatientInvoiceDetailByUniqueKey(patientId, uniqueKey.trim());
            if (detail != null) {
                return detail;
            }
            throw new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn.");
        }
        String normalizedSourceType = normalizeDetailSourceType(sourceType);
        if (normalizedSourceType != null) {
            PatientInvoiceDetailResponse detail = resolvePatientInvoiceDetailBySourceType(patientId, id, normalizedSourceType);
            if (detail != null) {
                return detail;
            }
            throw new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn.");
        }
        PatientInvoiceDetailResponse detail = buildMedicalRecordInvoiceDetail(
                invoiceRepository.findByIdAndMedicalRecordPatientId(id, patientId).orElse(null),
                patientId
        );
        if (detail != null) {
            return detail;
        }
        detail = buildAppointmentInvoiceDetail(
                appointmentRepository.findByIdAndPatientId(id, patientId).orElse(null),
                patientId
        );
        if (detail != null) {
            return detail;
        }
        detail = buildServicePackageInvoiceDetail(
                servicePackageBookingRepository.findByIdAndPatientId(id, patientId).orElse(null)
        );
        if (detail != null) {
            return detail;
        }
        throw new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn.");
    }

    public FinanceSummaryResponse buildSummary(List<InvoiceResponse> invoices) {
        return financeStatsService.buildSummary(invoices);
    }

    private List<InvoiceResponse> toAppointmentResponses(List<Appointment> appointments) {
        List<Appointment> billableAppointments = appointments == null
                ? List.of()
                : appointments.stream()
                .filter(Objects::nonNull)
                .filter(this::shouldExposeAppointmentBookingInvoice)
                .toList();

        Map<Integer, TransactionLog> latestLogsByAppointmentId = loadLatestLogsById(
                transactionLogRepository.findByAppointmentIdInOrderByCreatedAtDesc(
                        billableAppointments.stream().map(Appointment::getId).filter(Objects::nonNull).toList()
                ),
                TransactionLog::getAppointmentId,
                false
        );
        Map<Integer, TransactionLog> paidLogsByAppointmentId = loadLatestLogsById(
                transactionLogRepository.findByAppointmentIdInOrderByCreatedAtDesc(
                        billableAppointments.stream().map(Appointment::getId).filter(Objects::nonNull).toList()
                ),
                TransactionLog::getAppointmentId,
                true
        );

        List<Integer> appointmentIds = billableAppointments.stream()
                .map(Appointment::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Integer, PatientCancellationRequestSummary> cancellationByAppointmentId =
                appointmentCancellationService.getLatestCancellationSummariesByAppointmentIds(appointmentIds);

        List<InvoiceResponse> responses = new ArrayList<>();
        for (Appointment appointment : billableAppointments) {
            Integer appointmentId = appointment.getId();
            TransactionLog latestLog = appointmentId == null ? null : latestLogsByAppointmentId.get(appointmentId);
            TransactionLog paidLog = appointmentId == null ? null : paidLogsByAppointmentId.get(appointmentId);
            PatientCancellationRequestSummary cancellation = appointmentId == null
                    ? null
                    : cancellationByAppointmentId.get(appointmentId);
            responses.add(toAppointmentResponse(appointment, latestLog, paidLog, cancellation));
        }
        return responses;
    }

    private List<InvoiceResponse> toInvoiceResponses(List<Invoice> invoices) {
        List<Invoice> safeInvoices = invoices == null
                ? List.of()
                : invoices.stream().filter(Objects::nonNull).toList();
        Map<Integer, TransactionLog> paidLogsByInvoiceId = loadLatestLogsById(
                transactionLogRepository.findByInvoiceIdInOrderByCreatedAtDesc(
                        safeInvoices.stream().map(Invoice::getId).filter(Objects::nonNull).toList()
                ),
                TransactionLog::getInvoiceId,
                true
        );

        List<Integer> appointmentIds = safeInvoices.stream()
                .map(this::resolveAppointmentIdFromInvoice)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, PatientCancellationRequestSummary> cancellationByAppointmentId =
                appointmentCancellationService.getLatestCancellationSummariesByAppointmentIds(appointmentIds);

        List<InvoiceResponse> responses = new ArrayList<>();
        for (Invoice invoice : safeInvoices) {
            Integer invoiceId = invoice.getId();
            Integer appointmentId = resolveAppointmentIdFromInvoice(invoice);
            PatientCancellationRequestSummary cancellation = appointmentId == null
                    ? null
                    : cancellationByAppointmentId.get(appointmentId);
            responses.add(toInvoiceResponse(
                    invoice,
                    invoiceId == null ? null : paidLogsByInvoiceId.get(invoiceId),
                    cancellation
            ));
        }
        return responses;
    }

    private List<InvoiceResponse> toServicePackageResponses(List<ServicePackageBooking> bookings) {
        List<ServicePackageBooking> safeBookings = bookings == null
                ? List.of()
                : bookings.stream().filter(Objects::nonNull).toList();
        Map<Integer, TransactionLog> paidLogsByBookingId = loadLatestLogsById(
                transactionLogRepository.findByServicePackageBookingIdInOrderByCreatedAtDesc(
                        safeBookings.stream().map(ServicePackageBooking::getId).filter(Objects::nonNull).toList()
                ),
                TransactionLog::getServicePackageBookingId,
                true
        );

        List<InvoiceResponse> responses = new ArrayList<>();
        for (ServicePackageBooking booking : safeBookings) {
            Integer bookingId = booking.getId();
            responses.add(toServicePackageResponse(booking, bookingId == null ? null : paidLogsByBookingId.get(bookingId)));
        }
        return responses;
    }

    private InvoiceResponse toAppointmentResponse(Appointment appointment,
                                                    TransactionLog latestLog,
                                                    TransactionLog paidLog,
                                                    PatientCancellationRequestSummary cancellation) {
        InvoiceResponse response = new InvoiceResponse();
        ResolvedType resolvedType = AppointmentTypeCatalog.resolve(appointment);
        String invoiceTypeLabel = AppointmentTypeCatalog.appointmentBookingInvoiceLabel(resolvedType);
        String appointmentCode = safeText(appointment.getAppointmentCode());
        response.setUniqueKey("APPOINTMENT-" + appointment.getId());
        response.setId(appointment.getId());
        response.setSourceType(SOURCE_APPOINTMENT);
        response.setSourceId(appointment.getId());
        response.setInvoiceCode(appointmentCode);
        response.setInvoiceCategory(CATEGORY_APPOINTMENT_BOOKING);
        response.setInvoiceCategoryDisplay(invoiceTypeLabel);
        response.setInvoiceType(invoiceTypeLabel);
        response.setInvoiceTypeLabel(invoiceTypeLabel);
        response.setReferenceCode(appointmentCode);
        response.setRelatedName(resolvedType.label());
        response.setAppointmentId(appointment.getId());
        response.setAppointmentCode(appointmentCode);
        applyResolvedAppointmentType(response, resolvedType);
        applyAppointmentSchedule(response, appointment.getAppointmentDate());
        response.setPatientName(safeText(appointment.getPatientName()));
        response.setPatientFullName(safeText(appointment.getPatientName()));
        response.setPatientPhone(appointment.getPatient() == null ? null : safeText(appointment.getPatient().getPhone()));
        response.setDoctorName(safeText(appointment.getDoctorName()));
        response.setDoctorFullName(safeText(appointment.getDoctorName()));
        response.setConsultationFee(safeDoubleObject(appointment.getConsultationFee()));
        response.setMedicineFee(0.0);
        response.setServiceFee(0.0);
        response.setTotalAmount(safeDouble(appointment.getConsultationFee()));
        response.setAmount(safeDouble(appointment.getConsultationFee()));
        String paymentStatus = normalizePaymentStatus(appointment.getPaymentStatus());
        response.setStatus(paymentStatus);
        response.setPaymentStatus(paymentStatus);
        response.setPaymentStatusDisplay(toPaymentStatusDisplay(paymentStatus));
        response.setStatusLabel(toPaymentStatusDisplay(paymentStatus));
        String bookingStatus = normalizeBookingStatus(appointment.getStatus());
        response.setBookingStatus(bookingStatus);
        response.setBookingStatusDisplay(toBookingStatusDisplay(bookingStatus));
        response.setCanPayOnline(canPayOnline(appointment));
        applyAppointmentCancellationContext(response, appointment, cancellation);
        response.setCreatedAt(latestLog == null ? appointment.getAppointmentDate() : latestLog.getCreatedAt());
        response.setPaymentDate(paidLog == null ? null : paidLog.getCreatedAt());
        return response;
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice,
                                              TransactionLog paidLog,
                                              PatientCancellationRequestSummary cancellation) {
        if (invoice == null) {
            return null;
        }
        Appointment appointment = invoice.getAppointment() != null
                ? invoice.getAppointment()
                : (invoice.getMedicalRecord() == null ? null : invoice.getMedicalRecord().getAppointment());
        ResolvedType resolvedType = AppointmentTypeCatalog.resolve(appointment);
        Integer invoiceId = invoice.getId();
        Integer recordId = invoice.getMedicalRecord() == null ? null : invoice.getMedicalRecord().getId();
        String patientName = null;
        String patientPhone = null;
        String doctorName = null;
        if (invoice.getMedicalRecord() != null && invoice.getMedicalRecord().getPatient() != null) {
            patientName = invoice.getMedicalRecord().getPatient().getFullName();
            patientPhone = invoice.getMedicalRecord().getPatient().getPhone();
        }
        if (invoice.getMedicalRecord() != null && invoice.getMedicalRecord().getDoctor() != null) {
            doctorName = invoice.getMedicalRecord().getDoctor().getFullName();
        }

        double consultationFee = safeDouble(invoice.getConsultationFee());
        double medicineFee = safeDouble(invoice.getMedicineFee());
        double serviceFee = safeDouble(invoice.getServiceFee());
        double totalAmount = safeDouble(invoice.getTotalAmount());
        if (totalAmount <= 0 && (consultationFee + medicineFee + serviceFee) > 0) {
            totalAmount = consultationFee + medicineFee + serviceFee;
        }

        InvoiceResponse response = new InvoiceResponse();
        String category = resolvedType.reExamination() ? CATEGORY_FOLLOW_UP : CATEGORY_POST_EXAM;
        String invoiceTypeDisplay = AppointmentTypeCatalog.postExamInvoiceLabel(resolvedType);
        String invoiceCode = invoiceId == null ? null : "INV" + String.format("%06d", invoiceId);
        String appointmentCode = appointment == null ? null : safeText(appointment.getAppointmentCode());
        response.setUniqueKey("MEDICAL_RECORD_INVOICE-" + invoiceId);
        response.setId(invoiceId);
        response.setSourceType(SOURCE_INVOICE);
        response.setSourceId(invoiceId);
        response.setInvoiceCode(invoiceCode);
        response.setInvoiceCategory(category);
        response.setInvoiceCategoryDisplay(invoiceTypeDisplay);
        response.setInvoiceType(invoiceTypeDisplay);
        response.setInvoiceTypeLabel(invoiceTypeDisplay);
        response.setReferenceCode(appointmentCode != null ? appointmentCode : invoiceCode);
        response.setRelatedName(resolvedType.label());
        response.setRecordId(recordId);
        response.setMedicalRecordId(recordId);
        response.setAppointmentId(appointment == null ? null : appointment.getId());
        response.setAppointmentCode(appointmentCode);
        applyResolvedAppointmentType(response, resolvedType);
        applyAppointmentSchedule(response, appointment == null ? null : appointment.getAppointmentDate());
        response.setPatientName(safeText(patientName));
        response.setPatientFullName(safeText(patientName));
        response.setPatientPhone(safeText(patientPhone));
        response.setDoctorName(safeText(doctorName));
        response.setDoctorFullName(safeText(doctorName));
        response.setConsultationFee(consultationFee);
        response.setMedicineFee(medicineFee);
        response.setServiceFee(serviceFee);
        response.setTotalAmount(totalAmount);
        response.setAmount(totalAmount);
        String paymentStatus = normalizePaymentStatus(invoice.getStatus());
        response.setStatus(paymentStatus);
        response.setPaymentStatus(paymentStatus);
        response.setPaymentStatusDisplay(toPaymentStatusDisplay(paymentStatus));
        response.setStatusLabel(toPaymentStatusDisplay(paymentStatus));
        String bookingStatus = normalizeBookingStatus(appointment == null ? null : appointment.getStatus());
        response.setBookingStatus(bookingStatus);
        response.setBookingStatusDisplay(toBookingStatusDisplay(bookingStatus));
        response.setCanPayOnline(canPayOnline(invoice));
        applyAppointmentCancellationContext(response, appointment, cancellation);
        response.setCreatedAt(invoice.getCreatedAt());
        response.setPaymentDate(paidLog == null ? null : paidLog.getCreatedAt());
        return response;
    }

    private InvoiceResponse toServicePackageResponse(ServicePackageBooking booking, TransactionLog paidLog) {
        InvoiceResponse response = new InvoiceResponse();
        String bookingCode = safeText(booking.getBookingCode());
        String packageName = booking.getServicePackage() == null ? null : safeText(booking.getServicePackage().getName());
        response.setUniqueKey("SERVICE_PACKAGE-" + booking.getId());
        response.setId(booking.getId());
        response.setSourceType(SOURCE_SERVICE_PACKAGE);
        response.setSourceId(booking.getId());
        response.setInvoiceCode(bookingCode);
        response.setInvoiceCategory(CATEGORY_SERVICE_PACKAGE);
        String invoiceTypeLabel = AppointmentTypeCatalog.INVOICE_LABEL_SERVICE_PACKAGE;
        response.setInvoiceCategoryDisplay(invoiceTypeLabel);
        response.setInvoiceType(invoiceTypeLabel);
        response.setInvoiceTypeLabel(invoiceTypeLabel);
        response.setReferenceCode(bookingCode);
        response.setRelatedName(packageName);
        ResolvedType serviceType = AppointmentTypeCatalog.servicePackage(null);
        response.setAppointmentType(serviceType.code());
        response.setAppointmentTypeLabel(serviceType.label());
        response.setAppointmentTypeDisplay(serviceType.label());
        response.setIsReExamination(false);
        applyAppointmentSchedule(response, booking.getBookingDate() == null ? null : booking.getBookingDate().atTime(
                booking.getBookingTime() == null ? LocalTime.MIDNIGHT : booking.getBookingTime()
        ));
        response.setServicePackageBookingId(booking.getId());
        response.setServicePackageBookingCode(safeText(booking.getBookingCode()));
        response.setServicePackageName(booking.getServicePackage() == null ? null : safeText(booking.getServicePackage().getName()));
        response.setPatientName(booking.getPatient() == null ? null : safeText(booking.getPatient().getFullName()));
        response.setPatientFullName(booking.getPatient() == null ? null : safeText(booking.getPatient().getFullName()));
        response.setPatientPhone(booking.getPatient() == null ? null : safeText(booking.getPatient().getPhone()));
        response.setConsultationFee(0.0);
        response.setMedicineFee(0.0);
        response.setServiceFee(safeDouble(booking.getTotalAmount()));
        response.setTotalAmount(safeDouble(booking.getTotalAmount()));
        response.setAmount(safeDouble(booking.getTotalAmount()));
        String paymentStatus = normalizePaymentStatus(booking.getPaymentStatus());
        response.setStatus(paymentStatus);
        response.setPaymentStatus(paymentStatus);
        response.setPaymentStatusDisplay(toPaymentStatusDisplay(paymentStatus));
        response.setStatusLabel(toPaymentStatusDisplay(paymentStatus));
        String bookingStatus = normalizeServicePackageBookingStatus(booking.getStatus());
        response.setBookingStatus(bookingStatus);
        response.setBookingStatusDisplay(toServicePackageBookingStatusDisplay(bookingStatus));
        response.setCanPayOnline(canPayOnline(booking));
        response.setCreatedAt(booking.getCreatedAt());
        response.setPaymentDate(paidLog == null ? null : paidLog.getCreatedAt());
        return response;
    }

    private List<InvoiceResponse> filterInvoices(List<InvoiceResponse> invoices, String keyword, String status, String category) {
        return filterInvoices(invoices, keyword, status, category, null);
    }

    private List<InvoiceResponse> filterInvoices(List<InvoiceResponse> invoices,
                                                 String keyword,
                                                 String status,
                                                 String category,
                                                 String type) {
        String keywordNorm = normalizeFilter(keyword);
        String statusNorm = normalizeStatusFilter(status);
        String categoryNorm = normalizeCategoryFilter(firstNonBlank(category, type));
        String typeNorm = normalizeTypeFilter(type);
        if (keywordNorm == null && statusNorm == null && categoryNorm == null && typeNorm == null) {
            return sortInvoices(invoices);
        }

        List<InvoiceResponse> filtered = new ArrayList<>();
        for (InvoiceResponse invoice : invoices) {
            if (invoice == null) {
                continue;
            }
            if (statusNorm != null) {
                String invoiceStatus = normalizeFilter(invoice.getStatus());
                if (invoiceStatus == null || !invoiceStatus.equals(statusNorm)) {
                    continue;
                }
            }
            if (!matchesCategoryFilter(invoice, categoryNorm)) {
                continue;
            }
            if (!matchesTypeFilter(invoice, typeNorm)) {
                continue;
            }
            if (keywordNorm != null && !containsKeyword(invoice, keywordNorm)) {
                continue;
            }
            filtered.add(invoice);
        }
        return sortInvoices(filtered);
    }

    private List<InvoiceResponse> sortInvoices(List<InvoiceResponse> invoices) {
        List<InvoiceResponse> sorted = new ArrayList<>(invoices == null ? List.of() : invoices);
        sorted.sort((a, b) -> {
            LocalDateTime aTime = resolveSortTime(a);
            LocalDateTime bTime = resolveSortTime(b);
            if (aTime == null && bTime == null) {
                return 0;
            }
            if (aTime == null) {
                return 1;
            }
            if (bTime == null) {
                return -1;
            }
            return bTime.compareTo(aTime);
        });
        return sorted;
    }

    private boolean containsKeyword(InvoiceResponse invoice, String keywordNorm) {
        return contains(normalizeFilter(invoice.getInvoiceCode()), keywordNorm)
                || contains(normalizeFilter(invoice.getPatientName()), keywordNorm)
                || contains(normalizeFilter(invoice.getPatientPhone()), keywordNorm)
                || contains(normalizeFilter(invoice.getDoctorName()), keywordNorm)
                || contains(normalizeFilter(invoice.getAppointmentCode()), keywordNorm)
                || contains(normalizeFilter(invoice.getServicePackageBookingCode()), keywordNorm)
                || contains(normalizeFilter(invoice.getServicePackageName()), keywordNorm)
                || contains(normalizeFilter(invoice.getInvoiceCategoryDisplay()), keywordNorm)
                || contains(normalizeFilter(invoice.getRecordId() == null ? null : String.valueOf(invoice.getRecordId())), keywordNorm)
                || contains(normalizeFilter(invoice.getId() == null ? null : String.valueOf(invoice.getId())), keywordNorm);
    }

    private boolean contains(String text, String keyword) {
        return text != null && keyword != null && text.contains(keyword);
    }

    private boolean isPaidStatus(String status) {
        String normalized = normalizeFilter(status);
        return normalized != null
                && ("paid".equals(normalized)
                || "da_thanh_toan".equals(normalized)
                || "thanh_toan_thanh_cong".equals(normalized));
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String normalized = Normalizer.normalize(trimmed.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\u0111', 'd')
                .replace(' ', '_')
                .replace('-', '_')
                .replaceAll("[^a-z0-9_]", "")
                .replaceAll("_+", "_");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeStatusFilter(String status) {
        String normalized = normalizeFilter(status);
        if (normalized == null || "all".equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private String normalizeTypeFilter(String type) {
        String normalized = normalizeFilter(type);
        if (normalized == null || "all".equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private String normalizeCategoryFilter(String category) {
        String normalized = normalizeFilter(category);
        if (normalized == null || "all".equals(normalized)) {
            return null;
        }
        return switch (normalized) {
            case "appointment", "appointment_booking", "booking", "kham_benh", "hoa_don_kham_benh" -> CATEGORY_APPOINTMENT_BOOKING;
            case "post_exam", "after_exam", "sau_kham", "hoa_don_sau_kham", "medical_record", "record", "invoice" -> CATEGORY_POST_EXAM;
            case "follow_up", "tai_kham", "hoa_don_tai_kham" -> CATEGORY_FOLLOW_UP;
            case "service_package", "goi_dich_vu", "hoa_don_goi_dich_vu", "package" -> CATEGORY_SERVICE_PACKAGE;
            default -> normalized.toUpperCase(Locale.ROOT);
        };
    }

    private boolean matchesCategoryFilter(InvoiceResponse invoice, String categoryNorm) {
        if (categoryNorm == null) {
            return true;
        }
        if (CATEGORY_POST_EXAM.equals(categoryNorm) || "MEDICAL_RECORD".equals(categoryNorm)) {
            return CATEGORY_POST_EXAM.equals(invoice.getInvoiceCategory())
                    || CATEGORY_FOLLOW_UP.equals(invoice.getInvoiceCategory());
        }
        String invoiceCategory = normalizeCategoryFilter(invoice.getInvoiceCategory());
        return categoryNorm.equals(invoiceCategory);
    }

    private boolean matchesTypeFilter(InvoiceResponse invoice, String typeNorm) {
        if (typeNorm == null) {
            return true;
        }
        return switch (typeNorm) {
            case "appointment", "appointment_booking", "booking", "kham_benh", "hoa_don_kham_benh" ->
                    SOURCE_APPOINTMENT.equals(invoice.getSourceType());
            case "medical_record", "record", "invoice", "post_exam", "after_exam", "sau_kham", "hoa_don_sau_kham",
                    "follow_up", "tai_kham", "hoa_don_tai_kham" ->
                    SOURCE_INVOICE.equals(invoice.getSourceType());
            case "service_package", "goi_dich_vu", "hoa_don_goi_dich_vu", "package" ->
                    SOURCE_SERVICE_PACKAGE.equals(invoice.getSourceType());
            default -> matchesCategoryFilter(invoice, normalizeCategoryFilter(typeNorm));
        };
    }

    private String normalizePaymentStatus(String value) {
        String normalized = normalizeFilter(value);
        if (normalized == null) {
            return "UNPAID";
        }
        if (normalized.contains("paid") && !normalized.contains("unpaid")) {
            return "PAID";
        }
        if (normalized.contains("fail")) {
            return "FAILED";
        }
        if (normalized.contains("cancel")) {
            return "CANCELLED";
        }
        if (normalized.contains("pending")) {
            return "PENDING";
        }
        if (normalized.contains("unpaid") || normalized.contains("chua_thanh_toan")) {
            return "UNPAID";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeBookingStatus(String value) {
        String normalized = normalizeFilter(value);
        if (normalized == null) {
            return "PENDING";
        }
        if (normalized.contains("cancel")) {
            return "CANCELLED";
        }
        if (normalized.contains("completed") || normalized.contains("da_kham")) {
            return "COMPLETED";
        }
        if (normalized.contains("confirm")) {
            return "CONFIRMED";
        }
        return "PENDING";
    }

    private String normalizeServicePackageBookingStatus(String value) {
        String normalized = normalizeFilter(value);
        if (normalized == null) {
            return "PENDING_PAYMENT";
        }
        if (normalized.contains("cancel")) {
            return "CANCELLED";
        }
        if (normalized.contains("completed")) {
            return "COMPLETED";
        }
        if (normalized.contains("received")) {
            return "RECEIVED";
        }
        if (normalized.contains("paid")) {
            return "PAID";
        }
        return "PENDING_PAYMENT";
    }

    private String normalizeAppointmentType(String value) {
        String normalized = normalizeFilter(value);
        if (normalized != null && normalized.contains("tai_kham")) {
            return "T\u00e1i kh\u00e1m";
        }
        return "Kh\u00e1m b\u1ec7nh";
    }

    private String toPaymentStatusDisplay(String paymentStatus) {
        return switch (normalizePaymentStatus(paymentStatus)) {
            case "PAID" -> "\u0110\u00e3 thanh to\u00e1n";
            case "FAILED" -> "Thanh to\u00e1n th\u1ea5t b\u1ea1i";
            case "CANCELLED" -> "\u0110\u00e3 h\u1ee7y";
            case "PENDING" -> "\u0110ang ch\u1edd thanh to\u00e1n";
            default -> "Ch\u01b0a thanh to\u00e1n";
        };
    }

    private String toBookingStatusDisplay(String bookingStatus) {
        return switch (normalizeBookingStatus(bookingStatus)) {
            case "COMPLETED" -> "\u0110\u00e3 kh\u00e1m";
            case "CANCELLED" -> "H\u1ee7y l\u1ecbch";
            case "CONFIRMED" -> "\u0110\u00e3 x\u00e1c nh\u1eadn";
            default -> "Ch\u01b0a kh\u00e1m";
        };
    }

    private String toServicePackageBookingStatusDisplay(String bookingStatus) {
        return switch (normalizeServicePackageBookingStatus(bookingStatus)) {
            case "PAID" -> "\u0110\u00e3 thanh to\u00e1n";
            case "RECEIVED" -> "\u0110\u00e3 ti\u1ebfp nh\u1eadn";
            case "COMPLETED" -> "\u0110\u00e3 ho\u00e0n th\u00e0nh";
            case "CANCELLED" -> "\u0110\u00e3 h\u1ee7y";
            default -> "Ch\u1edd thanh to\u00e1n";
        };
    }

    private boolean shouldExposeAppointmentBookingInvoice(Appointment appointment) {
        if (appointment == null || appointment.getId() == null) {
            return false;
        }
        // Tái khám dùng hóa đơn riêng trong bảng invoices, không map từ appointment booking.
        if (AppointmentTypeCatalog.isReExamination(appointment)) {
            return false;
        }
        return true;
    }

    private void applyAppointmentSchedule(InvoiceResponse response, LocalDateTime appointmentDateTime) {
        if (appointmentDateTime == null) {
            return;
        }
        response.setAppointmentDate(appointmentDateTime.toLocalDate());
        response.setAppointmentTime(appointmentDateTime.toLocalTime());
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private boolean canPayOnline(Appointment appointment) {
        if (appointment == null || AppointmentTypeCatalog.isReExamination(appointment)) {
            return false;
        }
        String appointmentStatus = FinanceInvoiceRules.normalizeAppointmentStatus(appointment.getStatus());
        if (appointmentStatus != null && FinanceInvoiceRules.isCancelledAppointmentStatus(appointmentStatus)) {
            return false;
        }
        String paymentStatus = normalizePaymentStatus(appointment.getPaymentStatus());
        String bookingStatus = normalizeBookingStatus(appointment.getStatus());
        return ("UNPAID".equals(paymentStatus) || "PENDING".equals(paymentStatus))
                && !"CANCELLED".equals(bookingStatus)
                && !"COMPLETED".equals(bookingStatus)
                && safeDouble(appointment.getConsultationFee()) > 0;
    }

    private boolean canPayOnline(Invoice invoice) {
        if (invoice == null) {
            return false;
        }
        String paymentStatus = normalizePaymentStatus(invoice.getStatus());
        double payableAmount = safeDouble(invoice.getTotalAmount());
        if (payableAmount <= 0) {
            payableAmount = safeDouble(invoice.getConsultationFee())
                    + safeDouble(invoice.getMedicineFee())
                    + safeDouble(invoice.getServiceFee());
        }
        return ("UNPAID".equals(paymentStatus) || "PENDING".equals(paymentStatus))
                && payableAmount > 0;
    }

    private boolean canPayOnline(ServicePackageBooking booking) {
        if (booking == null) {
            return false;
        }
        String paymentStatus = normalizePaymentStatus(booking.getPaymentStatus());
        String bookingStatus = normalizeServicePackageBookingStatus(booking.getStatus());
        return ("UNPAID".equals(paymentStatus) || "PENDING".equals(paymentStatus))
                && !"CANCELLED".equals(bookingStatus)
                && safeDouble(booking.getTotalAmount()) > 0;
    }

    private boolean isFollowUpType(String type) {
        return AppointmentTypeCatalog.isReExaminationCodeOrLabel(type);
    }

    private void applyResolvedAppointmentType(InvoiceResponse response, ResolvedType resolvedType) {
        if (response == null || resolvedType == null) {
            return;
        }
        response.setAppointmentType(resolvedType.code());
        response.setAppointmentTypeLabel(resolvedType.label());
        response.setAppointmentTypeDisplay(resolvedType.label());
        response.setIsReExamination(resolvedType.reExamination());
    }

    private Map<Integer, TransactionLog> loadLatestLogsById(
            List<TransactionLog> logs,
            Function<TransactionLog, Integer> idExtractor,
            boolean paidOnly
    ) {
        Map<Integer, TransactionLog> result = new HashMap<>();
        if (logs == null || logs.isEmpty()) {
            return result;
        }
        for (TransactionLog log : logs) {
            if (log == null) {
                continue;
            }
            Integer targetId = idExtractor.apply(log);
            if (targetId == null || result.containsKey(targetId)) {
                continue;
            }
            if (paidOnly && !isSuccessfulPaymentLog(log)) {
                continue;
            }
            result.put(targetId, log);
        }
        return result;
    }

    private boolean isSuccessfulPaymentLog(TransactionLog log) {
        if (log == null || log.getResponseCode() == null) {
            return false;
        }
        String responseCode = log.getResponseCode().trim().toUpperCase(Locale.ROOT);
        return SUCCESS_CODE.equals(responseCode) || MANUAL_PAID_CODE.equals(responseCode);
    }

    private TransactionLog resolvePaidInvoiceLog(Integer invoiceId) {
        if (invoiceId == null) {
            return null;
        }
        TransactionLog successLog = transactionLogRepository.findTopByInvoiceIdAndResponseCodeOrderByCreatedAtDesc(invoiceId, SUCCESS_CODE);
        if (successLog != null) {
            return successLog;
        }
        return transactionLogRepository.findTopByInvoiceIdAndResponseCodeOrderByCreatedAtDesc(invoiceId, MANUAL_PAID_CODE);
    }

    private LocalDateTime resolveSortTime(InvoiceResponse response) {
        if (response == null) {
            return null;
        }
        if (response.getPaymentDate() != null) {
            return response.getPaymentDate();
        }
        return response.getCreatedAt();
    }

    private PatientInvoiceDetailResponse resolvePatientInvoiceDetailByUniqueKey(Integer patientId, String uniqueKey) {
        if (uniqueKey.startsWith("APPOINTMENT-")) {
            Integer appointmentId = parseTrailingId(uniqueKey, "APPOINTMENT-");
            return buildAppointmentInvoiceDetail(
                    appointmentRepository.findByIdAndPatientId(appointmentId, patientId).orElse(null),
                    patientId
            );
        }
        if (uniqueKey.startsWith("MEDICAL_RECORD_INVOICE-")) {
            Integer invoiceId = parseTrailingId(uniqueKey, "MEDICAL_RECORD_INVOICE-");
            return buildMedicalRecordInvoiceDetail(
                    invoiceRepository.findByIdAndMedicalRecordPatientId(invoiceId, patientId).orElse(null),
                    patientId
            );
        }
        if (uniqueKey.startsWith("SERVICE_PACKAGE-")) {
            Integer bookingId = parseTrailingId(uniqueKey, "SERVICE_PACKAGE-");
            return buildServicePackageInvoiceDetail(
                    servicePackageBookingRepository.findByIdAndPatientId(bookingId, patientId).orElse(null)
            );
        }
        return null;
    }

    private PatientInvoiceDetailResponse resolvePatientInvoiceDetailBySourceType(Integer patientId,
                                                                                 Integer id,
                                                                                 String sourceType) {
        return switch (sourceType) {
            case SOURCE_APPOINTMENT -> buildAppointmentInvoiceDetail(
                    appointmentRepository.findByIdAndPatientId(id, patientId).orElse(null),
                    patientId
            );
            case SOURCE_INVOICE, SOURCE_MEDICAL_RECORD -> buildMedicalRecordInvoiceDetail(
                    invoiceRepository.findByIdAndMedicalRecordPatientId(id, patientId).orElse(null),
                    patientId
            );
            case SOURCE_SERVICE_PACKAGE -> buildServicePackageInvoiceDetail(
                    servicePackageBookingRepository.findByIdAndPatientId(id, patientId).orElse(null)
            );
            default -> null;
        };
    }

    private PatientInvoiceDetailResponse buildAppointmentInvoiceDetail(Appointment appointment, Integer patientId) {
        if (appointment == null || appointment.getId() == null || AppointmentTypeCatalog.isReExamination(appointment)) {
            return null;
        }
        TransactionLog paidLog = resolvePaidAppointmentLog(appointment.getId());
        TransactionLog latestLog = transactionLogRepository
                .findByAppointmentIdInOrderByCreatedAtDesc(List.of(appointment.getId()))
                .stream()
                .findFirst()
                .orElse(null);
        MedicalRecord medicalRecord = medicalRecordRepository
                .findByAppointmentIdAndPatientId(appointment.getId(), patientId)
                .orElse(null);
        ResolvedType resolvedType = AppointmentTypeCatalog.resolve(appointment);
        String invoiceTypeLabel = AppointmentTypeCatalog.appointmentBookingInvoiceLabel(resolvedType);
        String paymentStatus = normalizePaymentStatus(appointment.getPaymentStatus());
        String appointmentCode = safeText(appointment.getAppointmentCode());
        LocalDateTime appointmentDateTime = appointment.getAppointmentDate();

        return PatientInvoiceDetailResponse.builder()
                .uniqueKey("APPOINTMENT-" + appointment.getId())
                .id(appointment.getId())
                .sourceType(SOURCE_APPOINTMENT)
                .sourceId(appointment.getId())
                .invoiceCode(appointmentCode)
                .invoiceType(invoiceTypeLabel)
                .invoiceTypeLabel(invoiceTypeLabel)
                .referenceCode(appointmentCode)
                .appointmentId(appointment.getId())
                .appointmentCode(appointmentCode)
                .medicalRecordId(medicalRecord == null ? null : medicalRecord.getId())
                .medicalRecordCode(medicalRecord == null ? null : safeText(medicalRecord.getMedicalRecordCode()))
                .examType(resolvedType.label())
                .appointmentType(resolvedType.code())
                .appointmentTypeLabel(resolvedType.label())
                .isReExamination(resolvedType.reExamination())
                .patientName(safeText(appointment.getPatientName()))
                .doctorName(safeText(appointment.getDoctorName()))
                .appointmentDate(appointmentDateTime == null ? null : appointmentDateTime.toLocalDate())
                .appointmentTime(appointmentDateTime == null ? null : appointmentDateTime.toLocalTime())
                .consultationFee(safeDoubleObject(appointment.getConsultationFee()))
                .medicineTotal(0.0)
                .serviceTotal(0.0)
                .totalAmount(safeDouble(appointment.getConsultationFee()))
                .paymentStatus(paymentStatus)
                .statusLabel(toPaymentStatusDisplay(paymentStatus))
                .bookingStatus(normalizeBookingStatus(appointment.getStatus()))
                .bookingStatusDisplay(toBookingStatusDisplay(appointment.getStatus()))
                .canPayOnline(canPayOnline(appointment))
                .createdAt(latestLog == null ? appointmentDateTime : latestLog.getCreatedAt())
                .paidAt(paidLog == null ? null : paidLog.getCreatedAt())
                .build();
    }

    private PatientInvoiceDetailResponse buildMedicalRecordInvoiceDetail(Invoice invoice, Integer patientId) {
        if (invoice == null || invoice.getId() == null) {
            return null;
        }
        MedicalRecord medicalRecord = invoice.getMedicalRecord();
        if (medicalRecord == null || medicalRecord.getPatient() == null
                || !Objects.equals(medicalRecord.getPatient().getId(), patientId)) {
            return null;
        }
        Appointment appointment = invoice.getAppointment() != null
                ? invoice.getAppointment()
                : medicalRecord.getAppointment();
        ResolvedType resolvedType = AppointmentTypeCatalog.resolve(appointment);
        String invoiceTypeDisplay = AppointmentTypeCatalog.postExamInvoiceLabel(resolvedType);
        Integer invoiceId = invoice.getId();
        String invoiceCode = "INV" + String.format("%06d", invoiceId);
        String appointmentCode = appointment == null ? null : safeText(appointment.getAppointmentCode());
        double medicineTotal = safeDouble(invoice.getMedicineFee());
        double serviceTotal = safeDouble(invoice.getServiceFee());
        double consultationFee = safeDouble(invoice.getConsultationFee());
        double totalAmount = safeDouble(invoice.getTotalAmount());
        if (totalAmount <= 0) {
            totalAmount = consultationFee + medicineTotal + serviceTotal;
        }
        String paymentStatus = normalizePaymentStatus(invoice.getStatus());
        TransactionLog paidLog = resolvePaidInvoiceLog(invoiceId);
        Integer recordId = medicalRecord.getId();
        LocalDateTime appointmentDateTime = appointment == null ? null : appointment.getAppointmentDate();

        return PatientInvoiceDetailResponse.builder()
                .uniqueKey("MEDICAL_RECORD_INVOICE-" + invoiceId)
                .id(invoiceId)
                .sourceType(SOURCE_MEDICAL_RECORD)
                .sourceId(invoiceId)
                .invoiceCode(invoiceCode)
                .invoiceType(invoiceTypeDisplay)
                .invoiceTypeLabel(invoiceTypeDisplay)
                .referenceCode(appointmentCode != null ? appointmentCode : invoiceCode)
                .appointmentId(appointment == null ? null : appointment.getId())
                .appointmentCode(appointmentCode)
                .medicalRecordId(recordId)
                .medicalRecordCode(safeText(medicalRecord.getMedicalRecordCode()))
                .examType(resolvedType.label())
                .appointmentType(resolvedType.code())
                .appointmentTypeLabel(resolvedType.label())
                .isReExamination(resolvedType.reExamination())
                .patientName(medicalRecord.getPatient() == null ? null : safeText(medicalRecord.getPatient().getFullName()))
                .doctorName(medicalRecord.getDoctor() == null ? null : safeText(medicalRecord.getDoctor().getFullName()))
                .appointmentDate(appointmentDateTime == null ? null : appointmentDateTime.toLocalDate())
                .appointmentTime(appointmentDateTime == null ? null : appointmentDateTime.toLocalTime())
                .consultationFee(consultationFee)
                .medicineTotal(medicineTotal)
                .serviceTotal(serviceTotal)
                .totalAmount(totalAmount)
                .paymentStatus(paymentStatus)
                .statusLabel(toPaymentStatusDisplay(paymentStatus))
                .bookingStatus(normalizeBookingStatus(appointment == null ? null : appointment.getStatus()))
                .bookingStatusDisplay(toBookingStatusDisplay(appointment == null ? null : appointment.getStatus()))
                .canPayOnline(canPayOnline(invoice))
                .createdAt(invoice.getCreatedAt())
                .paidAt(paidLog == null ? null : paidLog.getCreatedAt())
                .prescriptionItems(loadPrescriptionItems(recordId, patientId))
                .medicalServiceItems(loadMedicalServiceItems(recordId, patientId))
                .build();
    }

    private PatientInvoiceDetailResponse buildServicePackageInvoiceDetail(ServicePackageBooking booking) {
        if (booking == null || booking.getId() == null) {
            return null;
        }
        String bookingCode = safeText(booking.getBookingCode());
        String packageName = booking.getServicePackage() == null ? null : safeText(booking.getServicePackage().getName());
        String paymentStatus = normalizePaymentStatus(booking.getPaymentStatus());
        TransactionLog paidLog = resolvePaidServicePackageLog(booking.getId());
        LocalDateTime schedule = booking.getBookingDate() == null ? null : booking.getBookingDate().atTime(
                booking.getBookingTime() == null ? LocalTime.MIDNIGHT : booking.getBookingTime()
        );

        ResolvedType serviceType = AppointmentTypeCatalog.servicePackage(null);
        String invoiceTypeLabel = AppointmentTypeCatalog.INVOICE_LABEL_SERVICE_PACKAGE;
        return PatientInvoiceDetailResponse.builder()
                .uniqueKey("SERVICE_PACKAGE-" + booking.getId())
                .id(booking.getId())
                .sourceType(SOURCE_SERVICE_PACKAGE)
                .sourceId(booking.getId())
                .invoiceCode(bookingCode)
                .invoiceType(invoiceTypeLabel)
                .invoiceTypeLabel(invoiceTypeLabel)
                .referenceCode(bookingCode)
                .examType(serviceType.label())
                .appointmentType(serviceType.code())
                .appointmentTypeLabel(serviceType.label())
                .isReExamination(false)
                .patientName(booking.getPatient() == null ? null : safeText(booking.getPatient().getFullName()))
                .packageBookingCode(bookingCode)
                .servicePackageName(packageName)
                .consultationFee(0.0)
                .medicineTotal(0.0)
                .serviceTotal(safeDouble(booking.getTotalAmount()))
                .totalAmount(safeDouble(booking.getTotalAmount()))
                .paymentStatus(paymentStatus)
                .statusLabel(toPaymentStatusDisplay(paymentStatus))
                .bookingStatus(normalizeServicePackageBookingStatus(booking.getStatus()))
                .bookingStatusDisplay(toServicePackageBookingStatusDisplay(booking.getStatus()))
                .canPayOnline(canPayOnline(booking))
                .createdAt(booking.getCreatedAt())
                .paidAt(paidLog == null ? null : paidLog.getCreatedAt())
                .appointmentDate(schedule == null ? null : schedule.toLocalDate())
                .appointmentTime(schedule == null ? null : schedule.toLocalTime())
                .build();
    }

    private List<PatientInvoiceDetailResponse.PrescriptionItem> loadPrescriptionItems(Integer recordId, Integer patientId) {
        if (recordId == null || patientId == null) {
            return List.of();
        }
        return prescriptionDetailRepository.findPatientMedicineRowsByRecordId(recordId, patientId).stream()
                .map(row -> {
                    Integer quantity = asInteger(row[3]);
                    double unitPrice = safeDouble(asDouble(row[6]));
                    int safeQuantity = quantity == null ? 0 : quantity;
                    return PatientInvoiceDetailResponse.PrescriptionItem.builder()
                            .medicineName(asString(row[1]))
                            .quantity(quantity)
                            .unit(asString(row[2]))
                            .dosage(asString(row[4]))
                            .note(asString(row[5]))
                            .unitPrice(unitPrice)
                            .totalPrice(unitPrice * safeQuantity)
                            .build();
                })
                .toList();
    }

    private List<PatientInvoiceDetailResponse.MedicalServiceItem> loadMedicalServiceItems(Integer recordId, Integer patientId) {
        if (recordId == null || patientId == null) {
            return List.of();
        }
        return serviceDetailRepository.findPatientServiceRowsByRecordId(recordId, patientId).stream()
                .map(row -> {
                    Integer quantity = asInteger(row[2]);
                    double unitPrice = safeDouble(asDouble(row[4]));
                    int safeQuantity = quantity == null ? 0 : quantity;
                    String note = asString(row[3]);
                    return PatientInvoiceDetailResponse.MedicalServiceItem.builder()
                            .serviceName(asString(row[1]))
                            .quantity(quantity)
                            .note(note == null || note.isBlank() ? "-" : note)
                            .unitPrice(unitPrice)
                            .totalPrice(unitPrice * safeQuantity)
                            .build();
                })
                .toList();
    }

    private String resolveExamType(Appointment appointment) {
        return AppointmentTypeCatalog.resolve(appointment).label();
    }

    private String normalizeDetailSourceType(String sourceType) {
        String normalized = normalizeFilter(sourceType);
        if (normalized == null || "all".equals(normalized)) {
            return null;
        }
        return switch (normalized) {
            case "appointment", "appointment_booking", "booking" -> SOURCE_APPOINTMENT;
            case "invoice", "medical_record", "record", "post_exam", "follow_up" -> SOURCE_MEDICAL_RECORD;
            case "service_package", "package", "goi_dich_vu" -> SOURCE_SERVICE_PACKAGE;
            default -> normalized.toUpperCase(Locale.ROOT);
        };
    }

    private Integer parseTrailingId(String uniqueKey, String prefix) {
        try {
            return Integer.parseInt(uniqueKey.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private TransactionLog resolvePaidAppointmentLog(Integer appointmentId) {
        if (appointmentId == null) {
            return null;
        }
        TransactionLog successLog = transactionLogRepository
                .findTopByAppointmentIdAndResponseCodeOrderByCreatedAtDesc(appointmentId, SUCCESS_CODE);
        if (successLog != null) {
            return successLog;
        }
        return transactionLogRepository.findTopByAppointmentIdAndResponseCodeOrderByCreatedAtDesc(appointmentId, MANUAL_PAID_CODE);
    }

    private TransactionLog resolvePaidServicePackageLog(Integer bookingId) {
        if (bookingId == null) {
            return null;
        }
        List<TransactionLog> logs = transactionLogRepository
                .findByServicePackageBookingIdInOrderByCreatedAtDesc(List.of(bookingId));
        return logs == null ? null : logs.stream()
                .filter(this::isSuccessfulPaymentLog)
                .findFirst()
                .orElse(null);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private Double safeDoubleObject(Double value) {
        return value == null ? 0.0 : value;
    }

    private Integer resolveAppointmentIdFromInvoice(Invoice invoice) {
        if (invoice == null) {
            return null;
        }
        if (invoice.getAppointment() != null && invoice.getAppointment().getId() != null) {
            return invoice.getAppointment().getId();
        }
        if (invoice.getMedicalRecord() != null
                && invoice.getMedicalRecord().getAppointment() != null
                && invoice.getMedicalRecord().getAppointment().getId() != null) {
            return invoice.getMedicalRecord().getAppointment().getId();
        }
        return null;
    }

    private void applyAppointmentCancellationContext(InvoiceResponse response,
                                                     Appointment appointment,
                                                     PatientCancellationRequestSummary cancellation) {
        if (response == null || appointment == null) {
            return;
        }
        String appointmentStatus = FinanceInvoiceRules.normalizeAppointmentStatus(appointment.getStatus());
        response.setAppointmentStatus(appointmentStatus);
        response.setAppointmentStatusLabel(AppointmentCancellationService.resolveAppointmentStatusLabel(appointmentStatus));
        boolean cancelled = FinanceInvoiceRules.isCancelledAppointmentStatus(appointmentStatus);
        response.setIsCancelled(cancelled);
        if (cancellation != null) {
            response.setHasCancellationRequest(true);
            response.setCancellationRequestId(cancellation.getId());
            response.setCancellationStatus(cancellation.getStatus());
            response.setCancellationStatusLabel(cancellation.getStatusLabel());
        } else {
            response.setHasCancellationRequest(false);
        }
        String paymentLabel = AppointmentCancellationService.resolvePaymentStatusLabel(
                response.getPaymentStatus(),
                appointmentStatus
        );
        response.setPaymentStatusDisplay(paymentLabel);
        response.setStatusLabel(paymentLabel);
        if (cancelled) {
            response.setCanPayOnline(false);
        }
    }
}
