package com.medcare.clinic_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMedicineResponse {
    private Integer id;
    private String name;
    private String medicineCategory;
    private String category;
    private String manufacturer;
    private Integer quantity;
    private String unit;
    private Double price;
    private LocalDate expiryDate;
    private String status;
    private String dosage;
    private String description;
    private Boolean expired;
}
