package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "prescription_details")
public class PrescriptionDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nối với Hồ sơ bệnh án
    @ManyToOne
    @JoinColumn(name = "medical_record_id", nullable = false)
    private MedicalRecord medicalRecord;

    // Nối với Thuốc
    @ManyToOne
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private Integer quantity; // Số lượng (ví dụ: 10 viên)

    @Column(length = 255)
    private String dosage; // Cách dùng (ví dụ: Sáng 1, Tối 1 sau ăn)
}