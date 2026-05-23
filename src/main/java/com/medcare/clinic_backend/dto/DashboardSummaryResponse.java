package com.medcare.clinic_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long totalAppointments;
    private long activePatients;
    private long workingDoctors;
    private double monthlyRevenue;
    private int appointmentGrowthPercent;
    private int patientGrowthPercent;
    private int doctorGrowth;
    private double revenueGrowthPercent;
}
