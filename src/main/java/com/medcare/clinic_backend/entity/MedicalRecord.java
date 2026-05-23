package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Cuốn sổ của ai? (Nhiều trang record thuộc về 1 bệnh nhân)
    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Bác sĩ nào khám và ghi trang này?
    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    // Kết quả của cuộc hẹn nào? (1 cuộc hẹn sinh ra 1 trang record)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "examination_date", nullable = false)
    private LocalDate examinationDate; // Ngày khám (Ngày lật trang sổ)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String diagnosis; // Chẩn đoán ngày hôm đó

    @Column(columnDefinition = "TEXT")
    private String treatmentPlan; // Hướng điều trị

    @Column(columnDefinition = "TEXT")
    private String prescription; // Kê đơn thuốc

    @Column(columnDefinition = "TEXT")
    private String doctorAdvice; // Lời khuyên
}
