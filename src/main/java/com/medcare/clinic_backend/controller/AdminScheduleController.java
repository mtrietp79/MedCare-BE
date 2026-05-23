package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminScheduleController {

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping({"/api/admin/schedule", "/api/admin/schedules", "/api/admin/appointments"})
    public List<Appointment> getAllAppointmentsForAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date
    ) {
        String keywordNorm = normalizeFilter(keyword);
        String statusNorm = normalizeStatusFilter(status);
        LocalDate targetDate = parseDateOrNull(date);

        return appointmentService.getAllAppointments().stream()
                .filter(appointment -> matchesStatus(appointment, statusNorm))
                .filter(appointment -> matchesDate(appointment, targetDate))
                .filter(appointment -> matchesKeyword(appointment, keywordNorm))
                .sorted(Comparator.comparing(Appointment::getAppointmentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private boolean matchesStatus(Appointment appointment, String statusNorm) {
        if (statusNorm == null) {
            return true;
        }
        return statusNorm.equals(normalizeStatusFilter(appointment == null ? null : appointment.getStatus()));
    }

    private boolean matchesDate(Appointment appointment, LocalDate targetDate) {
        if (targetDate == null) {
            return true;
        }
        if (appointment == null || appointment.getAppointmentDate() == null) {
            return false;
        }
        return targetDate.equals(appointment.getAppointmentDate().toLocalDate());
    }

    private boolean matchesKeyword(Appointment appointment, String keywordNorm) {
        if (keywordNorm == null) {
            return true;
        }
        if (appointment == null) {
            return false;
        }

        String appointmentId = appointment.getId() == null ? null : String.valueOf(appointment.getId());
        String appointmentCode = appointment.getAppointmentCode();
        String patientName = appointment.getPatientName();
        String doctorName = appointment.getDoctorName();
        String specialtyName = appointment.getSpecialtyName();
        String serviceName = appointment.getServiceName();

        return contains(normalizeFilter(appointmentId), keywordNorm)
                || contains(normalizeFilter(appointmentCode), keywordNorm)
                || contains(normalizeFilter(patientName), keywordNorm)
                || contains(normalizeFilter(doctorName), keywordNorm)
                || contains(normalizeFilter(specialtyName), keywordNorm)
                || contains(normalizeFilter(serviceName), keywordNorm);
    }

    private boolean contains(String text, String keyword) {
        return text != null && keyword != null && text.contains(keyword);
    }

    private String normalizeStatusFilter(String value) {
        String normalized = normalizeFilter(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.equals("huy") || normalized.equals("huy_lich") || normalized.equals("cancelled") || normalized.equals("canceled")) {
            return "cancelled";
        }
        if (normalized.equals("da_kham") || normalized.equals("completed")) {
            return "completed";
        }
        if (normalized.equals("da_xac_nhan") || normalized.equals("confirmed")) {
            return "confirmed";
        }
        if (normalized.equals("cho_kham") || normalized.equals("pending")) {
            return "pending";
        }
        return normalized;
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String noAccent = Normalizer.normalize(trimmed, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String normalized = noAccent
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_')
                .replaceAll("[^a-z0-9_]", "")
                .replaceAll("_+", "_");
        return normalized.isBlank() ? null : normalized;
    }

    private LocalDate parseDateOrNull(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
