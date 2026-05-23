package com.medcare.clinic_backend.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDashboardResponse {
    private long todayAppointments;
    private long pendingAppointments;
    private long completedAppointmentsThisMonth;
    private double satisfactionRate;
}
