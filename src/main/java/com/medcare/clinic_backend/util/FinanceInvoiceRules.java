package com.medcare.clinic_backend.util;

import com.medcare.clinic_backend.dto.invoice.InvoiceResponse;

import java.util.Locale;
import java.util.Set;

public final class FinanceInvoiceRules {

    public static final String APPOINTMENT_STATUS_CANCEL_REQUESTED = "CANCEL_REQUESTED";
    public static final String APPOINTMENT_STATUS_CANCELLED = "CANCELLED";
    public static final String APPOINTMENT_STATUS_CANCEL_REJECTED = "CANCEL_REJECTED";

    private static final Set<String> CANCELLED_APPOINTMENT_STATUSES = Set.of(
            APPOINTMENT_STATUS_CANCEL_REQUESTED,
            APPOINTMENT_STATUS_CANCELLED,
            APPOINTMENT_STATUS_CANCEL_REJECTED
    );

    private static final Set<String> REVENUE_EXCLUDED_APPOINTMENT_STATUSES = Set.of(
            APPOINTMENT_STATUS_CANCEL_REQUESTED,
            APPOINTMENT_STATUS_CANCELLED
    );

    private static final Set<String> PAID_PAYMENT_STATUSES = Set.of("PAID", "PAID_ONLINE");

    private static final Set<String> REVENUE_EXCLUDED_PAYMENT_STATUSES = Set.of(
            "REFUNDED",
            "REFUND_PENDING",
            "CANCELLED",
            "VOID"
    );

    private FinanceInvoiceRules() {
    }

    public static boolean isCancelledAppointmentStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return CANCELLED_APPOINTMENT_STATUSES.contains(status.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean isPaidPaymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return PAID_PAYMENT_STATUSES.contains(status.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean countsTowardRevenue(InvoiceResponse invoice) {
        if (invoice == null) {
            return false;
        }
        String paymentStatus = normalizePaymentStatus(invoice.getPaymentStatus(), invoice.getStatus());
        if (!isPaidPaymentStatus(paymentStatus)) {
            return false;
        }
        if (REVENUE_EXCLUDED_PAYMENT_STATUSES.contains(paymentStatus)) {
            return false;
        }
        String appointmentStatus = normalizeAppointmentStatus(invoice.getAppointmentStatus());
        if (appointmentStatus != null && REVENUE_EXCLUDED_APPOINTMENT_STATUSES.contains(appointmentStatus)) {
            return false;
        }
        if (Boolean.TRUE.equals(invoice.getIsCancelled())) {
            return false;
        }
        return true;
    }

    public static boolean countsTowardPending(InvoiceResponse invoice) {
        if (invoice == null) {
            return false;
        }
        if (Boolean.TRUE.equals(invoice.getIsCancelled())) {
            return false;
        }
        String appointmentStatus = normalizeAppointmentStatus(invoice.getAppointmentStatus());
        if (appointmentStatus != null && isCancelledAppointmentStatus(appointmentStatus)) {
            return false;
        }
        String paymentStatus = normalizePaymentStatus(invoice.getPaymentStatus(), invoice.getStatus());
        if (isPaidPaymentStatus(paymentStatus)) {
            return false;
        }
        if (REVENUE_EXCLUDED_PAYMENT_STATUSES.contains(paymentStatus)) {
            return false;
        }
        return true;
    }

    public static boolean canPayInvoiceOnline(InvoiceResponse invoice) {
        if (invoice == null || Boolean.TRUE.equals(invoice.getIsCancelled())) {
            return false;
        }
        String appointmentStatus = normalizeAppointmentStatus(invoice.getAppointmentStatus());
        if (appointmentStatus != null && isCancelledAppointmentStatus(appointmentStatus)) {
            return false;
        }
        return Boolean.TRUE.equals(invoice.getCanPayOnline());
    }

    public static String normalizeAppointmentStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
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
        return upper;
    }

    public static String normalizePaymentStatus(String paymentStatus, String fallbackStatus) {
        String raw = paymentStatus != null && !paymentStatus.isBlank() ? paymentStatus : fallbackStatus;
        if (raw == null || raw.isBlank()) {
            return "UNPAID";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}
