package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.patient.*;
import com.medcare.clinic_backend.service.AdminPatientManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/patients")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminPatientManagementController {

    private final AdminPatientManagementService adminPatientManagementService;

    @GetMapping
    public Page<AdminPatientListItemResponse> getPatients(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        return adminPatientManagementService.getPatients(keyword, status, page, size, sort);
    }

    @GetMapping("/{patientId}")
    public AdminPatientDetailResponse getPatientDetail(@PathVariable Integer patientId) {
        return adminPatientManagementService.getPatientDetail(patientId);
    }

    @PatchMapping("/{patientId}/lock")
    public AdminPatientLockStatusResponse lockPatient(@PathVariable Integer patientId) {
        return adminPatientManagementService.lockPatient(patientId);
    }

    @PatchMapping("/{patientId}/unlock")
    public AdminPatientLockStatusResponse unlockPatient(@PathVariable Integer patientId) {
        return adminPatientManagementService.unlockPatient(patientId);
    }

    @PostMapping("/{patientId}/reset-password")
    public Map<String, Object> resetPassword(
            @PathVariable Integer patientId,
            @RequestBody(required = false) AdminPatientResetPasswordRequest request
    ) {
        String temporaryPassword = request == null ? null : request.getTemporaryPassword();
        return adminPatientManagementService.resetPassword(patientId, temporaryPassword);
    }

    @GetMapping("/stats")
    public AdminPatientStatsResponse getStats() {
        return adminPatientManagementService.getStats();
    }
}