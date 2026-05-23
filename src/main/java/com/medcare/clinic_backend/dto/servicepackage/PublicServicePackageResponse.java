package com.medcare.clinic_backend.dto.servicepackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicServicePackageResponse {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    private Integer durationMinutes;
    private String imageUrl;
}
