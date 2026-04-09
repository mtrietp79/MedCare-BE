package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "medical_services")
public class MedicalService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name; // Ví dụ: Siêu âm bụng tổng quát, Xét nghiệm máu (Công thức máu)

    @Column(nullable = false)
    private Double price; // Giá tiền dịch vụ

    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả dịch vụ (nếu có)
}