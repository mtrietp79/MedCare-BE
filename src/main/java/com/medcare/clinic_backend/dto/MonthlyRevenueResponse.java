package com.medcare.clinic_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenueResponse {
    private String month;
    private Double revenue;

    public Double getTotal() {
        return revenue;
    }

    public Double getValue() {
        return revenue;
    }

    public String getLabel() {
        return month;
    }
}
