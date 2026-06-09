package com.medcare.clinic_backend.dto.specialty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSpecialtyListItemResponse {
    private Integer id;
    private String name;
    private String description;
    private Long doctorCount;
    private Boolean isActive;
}
