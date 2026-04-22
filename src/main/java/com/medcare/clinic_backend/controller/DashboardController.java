package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    // API lấy các số liệu tổng quát (4 ô trên cùng trong ảnh)
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Map<String, Object> getSummary() {
        return dashboardService.getSummary();
    }

    // API lấy danh sách lịch hẹn mới nhất (cho cái bảng trong ảnh)
    @GetMapping("/recent-appointments")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<Appointment> getRecent() {
        return dashboardService.getRecentAppointments();
    }

    // API giả lập dữ liệu biểu đồ (Minh cần cái này để vẽ chart)
    @GetMapping("/revenue-chart")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Map<String, Object> getChartData() {
        Map<String, Object> chart = new HashMap<>();
        chart.put("labels", Arrays.asList("Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6"));
        chart.put("data", Arrays.asList(12, 19, 3, 5, 2, 3)); // Sau này viết logic thật sau
        return chart;
    }
}