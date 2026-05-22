package com.medcare.clinic_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

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

    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;

    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, CONFIRMED, COMPLETED, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "consultation_fee")
    private Double consultationFee;

    @Column(name = "payment_status")
    private String paymentStatus = "UNPAID"; // UNPAID, PAID

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
        return medicalService == null ? "Kham benh" : safeText(medicalService.getName());
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
