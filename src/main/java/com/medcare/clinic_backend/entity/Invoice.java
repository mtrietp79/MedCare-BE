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

    // Nối 1-1 với Hồ sơ bệnh án (1 lần khám sinh ra 1 hóa đơn phát sinh)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @Column(name = "medicine_fee")
    private Double medicineFee = 0.0;  // Tổng tiền thuốc

    @Column(name = "service_fee")
    private Double serviceFee = 0.0;   // Tổng tiền dịch vụ (xét nghiệm, X-quang...)

    @Column(name = "total_amount")
    private Double totalAmount = 0.0;  // Tổng cộng (medicineFee + serviceFee)

    // Trạng thái: UNPAID (Chưa thanh toán), PAID (Đã thanh toán)
    @Column(length = 20)
    private String status = "UNPAID";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
