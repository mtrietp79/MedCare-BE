package com.medcare.clinic_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

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
}