package com.medcare.clinic_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyPatientResponse {
    private String month;
    private long total;

    // Backward-compatible aliases for FE chart bindings.
    public long getCount() {
        return total;
    }

    public long getValue() {
        return total;
    }

    public long getPatients() {
        return total;
    }
}
