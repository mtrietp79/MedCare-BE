package com.medcare.clinic_backend.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceSummaryResponse {
    private double totalRevenue;
    private double monthlyRevenue;
    private double pendingAmount;
    private long paidCount;
    private long pendingCount;
    private long totalInvoices;
}
