package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.invoice.FinanceSummaryResponse;
import com.medcare.clinic_backend.dto.invoice.InvoiceResponse;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.ServicePackageBooking;
import com.medcare.clinic_backend.entity.TransactionLog;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.ServicePackageBookingRepository;
import com.medcare.clinic_backend.repository.TransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
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
        return getInvoiceResponsesForPatient(patientId, keyword, status, null);
    }

    public List<InvoiceResponse> getInvoiceResponsesForPatient(Integer patientId, String keyword, String status, String category) {
        List<InvoiceResponse> responses = new ArrayList<>();
        responses.addAll(toAppointmentResponses(appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(patientId)));
        responses.addAll(toInvoiceResponses(invoiceRepository.findByMedicalRecordPatientIdOrderByCreatedAtDesc(patientId)));
        responses.addAll(toServicePackageResponses(servicePackageBookingRepository.findByPatientIdOrderByCreatedAtDesc(patientId)));
        return filterInvoices(responses, keyword, status, category);
    }

    public InvoiceResponse getInvoiceResponseByRecordId(Integer recordId, Integer doctorIdOrNull) {
        Invoice invoice = (doctorIdOrNull == null)
                ? invoiceRepository.findByMedicalRecordId(recordId).orElse(null)
                : invoiceRepository.findByMedicalRecordIdAndMedicalRecordDoctorId(recordId, doctorIdOrNull).orElse(null);
        return invoice == null ? null : toInvoiceResponse(invoice, resolvePaidInvoiceLog(invoice.getId()));
    }

    public InvoiceResponse getInvoiceResponseByRecordIdForPatient(Integer recordId, Integer patientId) {
        Invoice invoice = invoiceRepository.findByMedicalRecordIdAndMedicalRecordPatientId(recordId, patientId).orElse(null);
        return invoice == null ? null : toInvoiceResponse(invoice, resolvePaidInvoiceLog(invoice.getId()));
    }

    public InvoiceResponse getInvoiceResponseByIdForPatient(Integer invoiceId, Integer patientId) {
        Invoice invoice = invoiceRepository.findByIdAndMedicalRecordPatientId(invoiceId, patientId).orElse(null);
        return invoice == null ? null : toInvoiceResponse(invoice, resolvePaidInvoiceLog(invoice.getId()));
    }

    public FinanceSummaryResponse buildSummary(List<InvoiceResponse> invoices) {
        LocalDateTime startOfCurrentMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startOfNextMonth = startOfCurrentMonth.plusMonths(1);

        double totalRevenue = 0.0;
        double monthlyRevenue = 0.0;
        double pendingAmount = 0.0;
        long paidCount = 0;
        long pendingCount = 0;

        for (InvoiceResponse invoice : invoices) {
            if (invoice == null) {
                continue;
            }
            boolean paid = isPaidStatus(invoice.getStatus());
            double totalAmount = safeDouble(invoice.getTotalAmount());
            if (paid) {
                paidCount++;
                totalRevenue += totalAmount;
                LocalDateTime revenueTime = invoice.getPaymentDate() != null ? invoice.getPaymentDate() : invoice.getCreatedAt();
                if (revenueTime != null && !revenueTime.isBefore(startOfCurrentMonth) && revenueTime.isBefore(startOfNextMonth)) {
                    monthlyRevenue += totalAmount;
                }
            } else {
                pendingCount++;
                pendingAmount += totalAmount;
            }
        }

        return new FinanceSummaryResponse(
                totalRevenue,
                monthlyRevenue,
                pendingAmount,
                paidCount,
                pendingCount,
                invoices == null ? 0 : invoices.size()
        );
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

        List<InvoiceResponse> responses = new ArrayList<>();
        for (Appointment appointment : billableAppointments) {
            Integer appointmentId = appointment.getId();
            TransactionLog latestLog = appointmentId == null ? null : latestLogsByAppointmentId.get(appointmentId);
            TransactionLog paidLog = appointmentId == null ? null : paidLogsByAppointmentId.get(appointmentId);
            responses.add(toAppointmentResponse(appointment, latestLog, paidLog));
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

        List<InvoiceResponse> responses = new ArrayList<>();
        for (Invoice invoice : safeInvoices) {
            Integer invoiceId = invoice.getId();
            responses.add(toInvoiceResponse(invoice, invoiceId == null ? null : paidLogsByInvoiceId.get(invoiceId)));
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

    private InvoiceResponse toAppointmentResponse(Appointment appointment, TransactionLog latestLog, TransactionLog paidLog) {
        InvoiceResponse response = new InvoiceResponse();
        response.setUniqueKey("APPOINTMENT_INVOICE_" + appointment.getId());
        response.setId(appointment.getId());
        response.setSourceType(SOURCE_APPOINTMENT);
        response.setSourceId(appointment.getId());
        response.setInvoiceCode(safeText(appointment.getAppointmentCode()));
        response.setInvoiceCategory(CATEGORY_APPOINTMENT_BOOKING);
        response.setInvoiceCategoryDisplay("H\u00f3a \u0111\u01a1n kh\u00e1m b\u1ec7nh");
        response.setAppointmentId(appointment.getId());
        response.setAppointmentCode(safeText(appointment.getAppointmentCode()));
        response.setAppointmentType(normalizeAppointmentType(appointment.getAppointmentType()));
        response.setAppointmentTypeDisplay(normalizeAppointmentType(appointment.getAppointmentType()));
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
        String bookingStatus = normalizeBookingStatus(appointment.getStatus());
        response.setBookingStatus(bookingStatus);
        response.setBookingStatusDisplay(toBookingStatusDisplay(bookingStatus));
        response.setCanPayOnline(canPayOnline(appointment));
        response.setCreatedAt(latestLog == null ? appointment.getAppointmentDate() : latestLog.getCreatedAt());
        response.setPaymentDate(paidLog == null ? null : paidLog.getCreatedAt());
        return response;
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice, TransactionLog paidLog) {
        if (invoice == null) {
            return null;
        }
        Appointment appointment = invoice.getAppointment() != null
                ? invoice.getAppointment()
                : (invoice.getMedicalRecord() == null ? null : invoice.getMedicalRecord().getAppointment());
        String appointmentType = normalizeAppointmentType(appointment == null ? null : appointment.getAppointmentType());
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
        String category = isFollowUpType(appointmentType) ? CATEGORY_FOLLOW_UP : CATEGORY_POST_EXAM;
        String prefix = isFollowUpType(appointmentType) ? "FOLLOW_UP_INVOICE_" : "POST_EXAM_INVOICE_";
        response.setUniqueKey(prefix + invoiceId);
        response.setId(invoiceId);
        response.setSourceType(SOURCE_INVOICE);
        response.setSourceId(invoiceId);
        response.setInvoiceCode(invoiceId == null ? null : "INV" + String.format("%06d", invoiceId));
        response.setInvoiceCategory(category);
        response.setInvoiceCategoryDisplay(isFollowUpType(appointmentType)
                ? "H\u00f3a \u0111\u01a1n t\u00e1i kh\u00e1m"
                : "H\u00f3a \u0111\u01a1n sau kh\u00e1m");
        response.setRecordId(recordId);
        response.setMedicalRecordId(recordId);
        response.setAppointmentId(appointment == null ? null : appointment.getId());
        response.setAppointmentCode(appointment == null ? null : safeText(appointment.getAppointmentCode()));
        response.setAppointmentType(appointmentType);
        response.setAppointmentTypeDisplay(appointmentType);
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
        String bookingStatus = normalizeBookingStatus(appointment == null ? null : appointment.getStatus());
        response.setBookingStatus(bookingStatus);
        response.setBookingStatusDisplay(toBookingStatusDisplay(bookingStatus));
        response.setCanPayOnline(canPayOnline(invoice));
        response.setCreatedAt(invoice.getCreatedAt());
        response.setPaymentDate(paidLog == null ? null : paidLog.getCreatedAt());
        return response;
    }

    private InvoiceResponse toServicePackageResponse(ServicePackageBooking booking, TransactionLog paidLog) {
        InvoiceResponse response = new InvoiceResponse();
        response.setUniqueKey("SERVICE_PACKAGE_BOOKING_" + booking.getId());
        response.setId(booking.getId());
        response.setSourceType(SOURCE_SERVICE_PACKAGE);
        response.setSourceId(booking.getId());
        response.setInvoiceCode(safeText(booking.getBookingCode()));
        response.setInvoiceCategory(CATEGORY_SERVICE_PACKAGE);
        response.setInvoiceCategoryDisplay("H\u00f3a \u0111\u01a1n g\u00f3i d\u1ecbch v\u1ee5");
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
        String bookingStatus = normalizeServicePackageBookingStatus(booking.getStatus());
        response.setBookingStatus(bookingStatus);
        response.setBookingStatusDisplay(toServicePackageBookingStatusDisplay(bookingStatus));
        response.setCanPayOnline(canPayOnline(booking));
        response.setCreatedAt(booking.getCreatedAt());
        response.setPaymentDate(paidLog == null ? null : paidLog.getCreatedAt());
        return response;
    }

    private List<InvoiceResponse> filterInvoices(List<InvoiceResponse> invoices, String keyword, String status, String category) {
        String keywordNorm = normalizeFilter(keyword);
        String statusNorm = normalizeFilter(status);
        String categoryNorm = normalizeCategoryFilter(category);
        if (keywordNorm == null && statusNorm == null && categoryNorm == null) {
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
            if (categoryNorm != null) {
                String invoiceCategory = normalizeCategoryFilter(invoice.getInvoiceCategory());
                if (invoiceCategory == null || !invoiceCategory.equals(categoryNorm)) {
                    continue;
                }
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

    private String normalizeCategoryFilter(String category) {
        String normalized = normalizeFilter(category);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "appointment", "appointment_booking", "booking", "kham_benh", "hoa_don_kham_benh" -> CATEGORY_APPOINTMENT_BOOKING;
            case "post_exam", "after_exam", "sau_kham", "hoa_don_sau_kham" -> CATEGORY_POST_EXAM;
            case "follow_up", "tai_kham", "hoa_don_tai_kham" -> CATEGORY_FOLLOW_UP;
            case "service_package", "goi_dich_vu", "hoa_don_goi_dich_vu", "package" -> CATEGORY_SERVICE_PACKAGE;
            default -> normalized.toUpperCase(Locale.ROOT);
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
        // Không hiển thị hóa đơn đặt lịch cho tái khám (vì sẽ dùng hóa đơn tái khám riêng)
        if (isFollowUpType(appointment.getAppointmentType())) {
            return false;
        }
        // Nếu đã có hóa đơn (sau khám) trong bảng invoices thì ẩn hóa đơn đặt lịch ban đầu
        return !invoiceRepository.existsByAppointmentId(appointment.getId());
    }

    private boolean canPayOnline(Appointment appointment) {
        if (appointment == null || isFollowUpType(appointment.getAppointmentType())) {
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
        String normalized = normalizeFilter(type);
        return normalized != null && normalized.contains("tai_kham");
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

    private String safeText(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private Double safeDoubleObject(Double value) {
        return value == null ? 0.0 : value;
    }
}
