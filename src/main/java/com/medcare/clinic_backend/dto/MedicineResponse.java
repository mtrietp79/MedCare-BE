package com.medcare.clinic_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineResponse {
    private Integer id;
    private String name;
    private String description;
    private String unit;
    private Double price;
    private Integer quantity;
    private String dosage;
    private String manufacturer;
    private LocalDate expiryDate;
    private String status;
    private String stockStatus;
    private String stockStatusLabel;
    private String expiryStatus;
    private String expiryStatusLabel;
    private Integer medicineCategoryId;
    private String medicineCategoryName;
    private String medicineCategory;
    private String category;
    private Boolean expired;
}
