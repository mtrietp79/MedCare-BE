package com.medcare.clinic_backend.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorMedicalServiceResponse {
    private Integer id;
    private String name;
    private Double price;
    private String description;
}
