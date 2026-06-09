package com.medcare.clinic_backend.util;

import com.medcare.clinic_backend.entity.Appointment;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Central mapping for appointment / booking business types.
 * DB stores Vietnamese labels; API exposes stable codes plus labels.
 */
public final class AppointmentTypeCatalog {

    public static final String CODE_EXAMINATION = "EXAMINATION";
    public static final String CODE_RE_EXAMINATION = "RE_EXAMINATION";
    public static final String CODE_SERVICE_PACKAGE = "SERVICE_PACKAGE";

    public static final String LABEL_EXAMINATION = "Kh\u00e1m b\u1ec7nh";
    public static final String LABEL_RE_EXAMINATION = "T\u00e1i kh\u00e1m";
    public static final String LABEL_SERVICE_PACKAGE = "G\u00f3i d\u1ecbch v\u1ee5";
    public static final String LABEL_SERVICE = "D\u1ecbch v\u1ee5";

    public static final String INVOICE_LABEL_EXAMINATION = "H\u00f3a \u0111\u01a1n kh\u00e1m b\u1ec7nh";
    public static final String INVOICE_LABEL_RE_EXAMINATION = "H\u00f3a \u0111\u01a1n t\u00e1i kh\u00e1m";
    public static final String INVOICE_LABEL_POST_EXAM = "H\u00f3a \u0111\u01a1n sau kh\u00e1m";
    public static final String INVOICE_LABEL_SERVICE_PACKAGE = "H\u00f3a \u0111\u01a1n g\u00f3i d\u1ecbch v\u1ee5";

    private AppointmentTypeCatalog() {
    }

    public record ResolvedType(
            String code,
            String label,
            boolean reExamination,
            Integer originalAppointmentId
    ) {
    }

    public static ResolvedType resolve(Appointment appointment) {
        if (appointment == null) {
            return examination(null);
        }
        if (isServicePackageAppointment(appointment)) {
            return servicePackage(null);
        }
        if (isReExamination(appointment)) {
            return reExamination(appointment.getParentAppointmentId());
        }
        return examination(null);
    }

    public static ResolvedType resolve(String storedType, Integer parentAppointmentId, String followUpNote) {
        if (isReExaminationCodeOrLabel(storedType) || parentAppointmentId != null || hasText(followUpNote)) {
            return reExamination(parentAppointmentId);
        }
        if (isServicePackageCodeOrLabel(storedType)) {
            return servicePackage(null);
        }
        return examination(null);
    }

    public static ResolvedType examination(Integer originalAppointmentId) {
        return new ResolvedType(CODE_EXAMINATION, LABEL_EXAMINATION, false, originalAppointmentId);
    }

    public static ResolvedType reExamination(Integer originalAppointmentId) {
        return new ResolvedType(CODE_RE_EXAMINATION, LABEL_RE_EXAMINATION, true, originalAppointmentId);
    }

    public static ResolvedType servicePackage(Integer originalAppointmentId) {
        return new ResolvedType(CODE_SERVICE_PACKAGE, LABEL_SERVICE_PACKAGE, false, originalAppointmentId);
    }

    public static boolean isReExamination(Appointment appointment) {
        if (appointment == null) {
            return false;
        }
        if (isReExaminationCodeOrLabel(appointment.getAppointmentType())) {
            return true;
        }
        if (appointment.getParentAppointmentId() != null) {
            return true;
        }
        return hasText(appointment.getFollowUpNote());
    }

    public static boolean isReExaminationCodeOrLabel(String value) {
        String folded = foldText(value);
        if (folded == null) {
            return false;
        }
        return folded.contains("taikham")
                || folded.contains("reexamination")
                || folded.contains("followup")
                || folded.contains("follow_up")
                || folded.contains("revisit")
                || folded.contains("re_visit");
    }

    public static boolean isServicePackageAppointment(Appointment appointment) {
        return appointment != null
                && appointment.getServicePackage() != null
                && appointment.getServicePackage().getId() != null;
    }

    public static boolean isServicePackageCodeOrLabel(String value) {
        String folded = foldText(value);
        if (folded == null) {
            return false;
        }
        return folded.contains("servicepackage")
                || folded.contains("goi")
                || folded.contains("package")
                || folded.equals("service")
                || folded.contains("dichvu");
    }

    public static String storageLabelForCode(String code) {
        if (CODE_RE_EXAMINATION.equals(code)) {
            return LABEL_RE_EXAMINATION;
        }
        if (CODE_SERVICE_PACKAGE.equals(code)) {
            return LABEL_SERVICE_PACKAGE;
        }
        return LABEL_EXAMINATION;
    }

    public static String appointmentBookingInvoiceLabel(ResolvedType type) {
        if (type != null && type.reExamination()) {
            return INVOICE_LABEL_RE_EXAMINATION;
        }
        return INVOICE_LABEL_EXAMINATION;
    }

    public static String postExamInvoiceLabel(ResolvedType type) {
        if (type != null && type.reExamination()) {
            return INVOICE_LABEL_RE_EXAMINATION;
        }
        return INVOICE_LABEL_POST_EXAM;
    }

    public static String foldText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('\u0111', 'd')
                .replace('\u0110', 'd')
                .replace(" ", "")
                .replace("_", "");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
