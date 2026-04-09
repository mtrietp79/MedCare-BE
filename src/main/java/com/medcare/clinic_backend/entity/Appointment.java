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

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // THÊM MỚI: Khóa ngoại nối đến Chuyên khoa
    // Cái này nên để nullable = false vì ít nhất phải biết khám khoa nào
    @ManyToOne
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    // Bác sĩ vẫn để nullable = true để lễ tân xếp sau
    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;

    @Column(nullable = false, length = 50)
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    // Trong file Appointment.java
    @Column(name = "consultation_fee")
    private Double consultationFee = 150000.0; // Giá mặc định hoặc lấy theo bác sĩ

    @Column(name = "payment_status")
    private String paymentStatus = "UNPAID"; // UNPAID, PAID_ONLINE
}