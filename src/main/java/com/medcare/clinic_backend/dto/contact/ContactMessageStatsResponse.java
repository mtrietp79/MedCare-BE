package com.medcare.clinic_backend.dto.contact;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContactMessageStatsResponse {
    private long total;
    private long newCount;
    private long inProgressCount;
    private long repliedCount;
    private long closedCount;
}
