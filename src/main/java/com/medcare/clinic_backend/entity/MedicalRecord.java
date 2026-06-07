package com.medcare.clinic_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "appointment_id", insertable = false, updatable = false)
    private Integer appointmentKey;

    @Column(name = "medical_record_code", length = 30, unique = true)
    private String medicalRecordCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_up_appointment_id")
    private Appointment followUpAppointment;

    @Column(name = "follow_up_appointment_id", insertable = false, updatable = false)
    private Integer followUpAppointmentKey;

    @Column(length = 50)
    private String type = "Khám bệnh";

    @Column(name = "examination_date", nullable = false)
    private LocalDate examinationDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String treatmentPlan;

    @Column(columnDefinition = "TEXT")
    private String prescription;

    @Column(columnDefinition = "TEXT")
    private String doctorAdvice;
}
