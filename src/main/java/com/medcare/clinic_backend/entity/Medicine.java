package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "medicines")
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String unit; // viên, gói, chai, vỉ...

    @Column(nullable = false)
    private Double price; // Giá bán mỗi đơn vị

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "dosage")
    private String dosage;

    @Column(name = "quantity")
    private Integer quantity = 0;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "status", length = 50)
    private String status;
}
