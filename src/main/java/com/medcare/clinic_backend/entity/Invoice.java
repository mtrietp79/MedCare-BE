package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(name = "consultation_fee")
    private Double consultationFee = 0.0;

    @Column(name = "medicine_fee")
    private Double medicineFee = 0.0;

    @Column(name = "service_fee")
    private Double serviceFee = 0.0;

    @Column(name = "total_amount")
    private Double totalAmount = 0.0;

    @Column(length = 20)
    private String status = "UNPAID";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
