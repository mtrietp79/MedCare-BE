package com.medcare.clinic_backend.dto;

import lombok.Data;

@Data
public class MedicineCategoryRequest {
    private String name;
    private String description;
    private Boolean isActive;
}
