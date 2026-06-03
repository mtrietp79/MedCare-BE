package com.medcare.clinic_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
    private String unit; // unit for quantity, e.g. vien/goi/chai

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_category_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MedicineCategory medicineCategory;

    @Column(name = "medicine_category", nullable = false, length = 100)
    private String legacyMedicineCategory = "Khác";

    @Column(nullable = false)
    private Double price;

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
