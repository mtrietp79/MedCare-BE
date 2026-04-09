package com.medcare.clinic_backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/test")
public class TestSecurityController {

    // 1. API này ai có thẻ Token cũng vào được (Không cần phân biệt chức vụ)
    @GetMapping("/all")
    public String allAccess() {
        return "Chào mừng bạn! Ai có Token cũng xem được dòng này.";
    }

    // 2. API này CHỈ BỆNH NHÂN mới được vào
    @GetMapping("/patient")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public String patientAccess() {
        return "Góc Bệnh Nhân: Đây là hồ sơ sức khỏe của bạn.";
    }

    // 3. API này CHỈ BÁC SĨ mới được vào
    @GetMapping("/doctor")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public String doctorAccess() {
        return "Góc Bác Sĩ: Chào bác sĩ, đây là công cụ kê đơn thuốc.";
    }
}