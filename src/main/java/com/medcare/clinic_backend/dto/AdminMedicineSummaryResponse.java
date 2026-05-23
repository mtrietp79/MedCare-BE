package com.medcare.clinic_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMedicineSummaryResponse {
    private long lowStockCount;
    private long outOfStockCount;
    private long expiredCount;
    private long total;
}
