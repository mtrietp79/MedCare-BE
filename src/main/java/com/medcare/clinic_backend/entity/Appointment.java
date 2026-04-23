package com.medcare.clinic_backend.entity;

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
}
