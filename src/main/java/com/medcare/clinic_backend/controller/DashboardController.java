package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.DashboardSummaryResponse;
import com.medcare.clinic_backend.dto.MonthlyPatientResponse;
import com.medcare.clinic_backend.dto.RecentAppointmentResponse;
import com.medcare.clinic_backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/monthly-patients")
    public List<MonthlyPatientResponse> getMonthlyPatients(@RequestParam(required = false) Integer year) {
        return dashboardService.getMonthlyPatients(year);
    }

    @GetMapping("/recent-appointments")
    public List<RecentAppointmentResponse> getRecentAppointments() {
        return dashboardService.getRecentAppointments();
    }
}
