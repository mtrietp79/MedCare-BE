package com.medcare.clinic_backend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MedicineRequest {
    private String name;
    private String description;
    private String unit;
    private Double price;
    private Integer quantity;
    private String dosage;
    private String manufacturer;
    private LocalDate expiryDate;
    private Integer medicineCategoryId;
    private String medicineCategory;
    private String category;
}
