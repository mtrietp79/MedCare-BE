package com.medcare.clinic_backend.dto.servicepackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminServicePackageResponse {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    private Integer durationMinutes;
    private String imageUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PublicServicePackageDetailResponse.Item> items;
}
