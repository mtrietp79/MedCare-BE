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

    public long getAppointments() {
        return totalAppointments;
    }

    public long getAppointmentCount() {
        return totalAppointments;
    }

    public long getPatients() {
        return activePatients;
    }

    public long getPatientCount() {
        return activePatients;
    }

    public long getTotalPatients() {
        return activePatients;
    }

    public long getDoctors() {
        return workingDoctors;
    }

    public long getDoctorCount() {
        return workingDoctors;
    }

    public long getActiveDoctors() {
        return workingDoctors;
    }

    public long getTotalDoctors() {
        return workingDoctors;
    }

    public double getRevenue() {
        return monthlyRevenue;
    }

    public double getTotalRevenue() {
        return monthlyRevenue;
    }

    public int getAppointmentGrowth() {
        return appointmentGrowthPercent;
    }

    public int getPatientGrowth() {
        return patientGrowthPercent;
    }

    public int getDoctorDelta() {
        return doctorGrowth;
    }

    public double getRevenueGrowth() {
        return revenueGrowthPercent;
    }
}
