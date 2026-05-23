package com.medcare.clinic_backend.dto.servicepackage;

import lombok.Data;

import java.util.List;

@Data
public class AdminServicePackageRequest {
    private String name;
    private String description;
    private Double price;
    private Integer durationMinutes;
    private String imageUrl;
    private Boolean isActive;
    private List<Integer> medicalServiceIds;
}
