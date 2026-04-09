package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "service_details")
public class ServiceDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nối với Hồ sơ bệnh án
    @ManyToOne
    @JoinColumn(name = "medical_record_id", nullable = false)
    private MedicalRecord medicalRecord;

    // Nối với Danh mục dịch vụ
    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private MedicalService medicalService;

    @Column(nullable = false)
    private Integer quantity = 1; // Mặc định là 1 lần thực hiện

    @Column(length = 255)
    private String result; // Kết quả xét nghiệm/siêu âm (Bác sĩ chuyên môn nhập vào sau)
}