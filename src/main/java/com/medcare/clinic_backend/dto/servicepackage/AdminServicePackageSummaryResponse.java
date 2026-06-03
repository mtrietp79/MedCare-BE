package com.medcare.clinic_backend.dto.servicepackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminServicePackageSummaryResponse {
    private long totalPackages;
    private long activePackages;
    private long inactivePackages;
    private long packagesWithBookings;
    private long packagesWithoutItems;
}
