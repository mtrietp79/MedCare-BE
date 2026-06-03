package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.feedback.MessageResponse;
import com.medcare.clinic_backend.dto.servicepackage.AdminServicePackageRequest;
import com.medcare.clinic_backend.dto.servicepackage.AdminServicePackageResponse;
import com.medcare.clinic_backend.dto.servicepackage.AdminServicePackageSummaryResponse;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.service.ServicePackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/service-packages")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminServicePackageController {

    @Autowired
    private ServicePackageService servicePackageService;

    @GetMapping
    public List<AdminServicePackageResponse> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean configured
    ) {
        return servicePackageService.getAllForAdmin(
                firstNonBlank(keyword, q, search),
                resolveActiveFilter(active, status),
                configured
        );
    }

    @GetMapping("/summary")
    public AdminServicePackageSummaryResponse getSummary() {
        return servicePackageService.getAdminSummary();
    }

    @GetMapping("/{id}")
    public AdminServicePackageResponse getById(@PathVariable Integer id) {
        return servicePackageService.getByIdForAdmin(id);
    }

    @PostMapping
    public AdminServicePackageResponse create(@RequestBody AdminServicePackageRequest request) {
        return servicePackageService.createForAdmin(request);
    }

    @PutMapping("/{id}")
    public AdminServicePackageResponse update(@PathVariable Integer id, @RequestBody AdminServicePackageRequest request) {
        return servicePackageService.updateForAdmin(id, request);
    }

    @PatchMapping("/{id}/active")
    public AdminServicePackageResponse setActive(@PathVariable Integer id, @RequestParam boolean active) {
        return servicePackageService.setActiveForAdmin(id, active);
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(@PathVariable Integer id) {
        return servicePackageService.deleteForAdmin(id);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Boolean resolveActiveFilter(Boolean active, String status) {
        if (active != null) {
            return active;
        }
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "ACTIVE", "TRUE", "DANG_HOAT_DONG", "DANG HOAT DONG" -> true;
            case "INACTIVE", "FALSE", "TAM_NGUNG", "TAM NGUNG" -> false;
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "Trang thai goi dich vu khong hop le.");
        };
    }
}
