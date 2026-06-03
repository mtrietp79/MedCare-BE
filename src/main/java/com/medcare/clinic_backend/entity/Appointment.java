package com.medcare.clinic_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Locale;

@Data
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "appointment_code", length = 30, unique = true)
    private String appointmentCode;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "medical_service_id")
    @JsonIgnoreProperties({"prescriptionItems"})
    private MedicalService medicalService;

    @ManyToOne
    @JoinColumn(name = "service_package_id")
    private ServicePackage servicePackage;

    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;

    @Column(name = "appointment_type", length = 50)
    private String appointmentType = "Kh\u00e1m b\u1ec7nh";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_appointment_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Appointment parentAppointment;

    @Column(name = "follow_up_note", columnDefinition = "TEXT")
    private String followUpNote;

    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, CONFIRMED, COMPLETED, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "consultation_fee")
    private Double consultationFee;

    @Column(name = "payment_status")
    private String paymentStatus = "UNPAID"; // UNPAID, PAID, PAID_ONLINE, FAILED, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String notes;

    public String getPatientName() {
        return patient == null ? "" : safeText(patient.getFullName());
    }

    public String getDoctorName() {
        return doctor == null ? "" : safeText(doctor.getFullName());
    }

    public String getSpecialtyName() {
        return specialty == null ? "" : safeText(specialty.getName());
    }

    public String getServiceName() {
        if (servicePackage != null) {
            return safeText(servicePackage.getName());
        }
        return medicalService == null ? "Kham benh" : safeText(medicalService.getName());
    }

    public String getStatusDisplay() {
        String normalized = normalizeStatus(status);
        return switch (normalized) {
            case "CANCELLED" -> "\u0110\u00e3 h\u1ee7y";
            case "COMPLETED" -> "\u0110\u00e3 kh\u00e1m";
            default -> "Ch\u01b0a kh\u00e1m";
        };
    }

    public String getPaymentStatusDisplay() {
        String normalized = normalizePaymentStatus(paymentStatus);
        return switch (normalized) {
            case "PAID" -> "\u0110\u00e3 thanh to\u00e1n";
            case "CANCELLED" -> "\u0110\u00e3 h\u1ee7y";
            case "FAILED" -> "Thanh to\u00e1n th\u1ea5t b\u1ea1i";
            default -> "Ch\u01b0a thanh to\u00e1n";
        };
    }

    public String getStatusColor() {
        String normalized = normalizeStatus(status);
        return switch (normalized) {
            case "CANCELLED" -> "red";
            case "COMPLETED" -> "green";
            default -> "yellow";
        };
    }

    private String normalizeStatus(String rawStatus) {
        if (rawStatus == null) {
            return "PENDING";
        }
        String upper = rawStatus.trim().toUpperCase(Locale.ROOT);
        if (upper.contains("CANCEL")) {
            return "CANCELLED";
        }
        if (upper.contains("COMPLETED")) {
            return "COMPLETED";
        }
        return "PENDING";
    }

    private String normalizePaymentStatus(String rawStatus) {
        if (rawStatus == null) {
            return "UNPAID";
        }
        String upper = rawStatus.trim().toUpperCase(Locale.ROOT);
        if ("PAID".equals(upper) || "PAID_ONLINE".equals(upper)) {
            return "PAID";
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

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
