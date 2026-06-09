package com.medcare.clinic_backend.dto.cancellation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCancellationRequestStatsResponse {
    private long total;
    private long pending;
    private long approved;
    private long rejected;
    private long refunded;
    private double totalRefundAmountPending;
    private double totalRefundAmountProcessed;
}
