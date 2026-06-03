package com.medcare.clinic_backend.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorMedicineResponse {
    private Integer id;
    private String name;
    private Integer medicineCategoryId;
    private String medicineCategoryName;
    private String medicineCategory;
    private String unit;
    private Integer quantity;
    private String dosage;
    private Double price;
    private String status;
}
