package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.DashboardSummaryResponse;
import com.medcare.clinic_backend.dto.MonthlyPatientResponse;
import com.medcare.clinic_backend.dto.MonthlyRevenueResponse;
import com.medcare.clinic_backend.dto.RecentAppointmentResponse;
import com.medcare.clinic_backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/dashboard", "/api/dashboard"})
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(Authentication authentication) {
        return dashboardService.getSummary();
    }

    @GetMapping("/monthly-patients")
    public List<MonthlyPatientResponse> getMonthlyPatients(
            @RequestParam(required = false) Integer year,
            Authentication authentication
    ) {
        return dashboardService.getMonthlyPatients(year);
    }

    @GetMapping("/revenue-chart")
    public List<MonthlyRevenueResponse> getRevenueChart(
            @RequestParam(required = false) Integer year,
            Authentication authentication
    ) {
        return dashboardService.getMonthlyRevenue(year);
    }

    @GetMapping("/recent-appointments")
    public List<RecentAppointmentResponse> getRecentAppointments(Authentication authentication) {
        return dashboardService.getRecentAppointments();
    }

    @GetMapping({"/report", "/report.xlsx"})
    public ResponseEntity<byte[]> exportDashboardReport(
            @RequestParam(required = false) Integer year,
            Authentication authentication
    ) {
        int targetYear = year == null ? java.time.LocalDate.now().getYear() : year;
        byte[] fileContent = dashboardService.exportDashboardReport(year);
        String filename = "medcare-dashboard-report-" + targetYear + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .body(fileContent);
    }
}
