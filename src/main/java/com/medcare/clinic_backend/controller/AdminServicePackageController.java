package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.servicepackage.AdminServicePackageRequest;
import com.medcare.clinic_backend.dto.servicepackage.AdminServicePackageResponse;
import com.medcare.clinic_backend.service.ServicePackageService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<AdminServicePackageResponse> getAll() {
        return servicePackageService.getAllForAdmin();
    }

    @PostMapping
    public AdminServicePackageResponse create(@RequestBody AdminServicePackageRequest request) {
        return servicePackageService.createForAdmin(request);
    }

    @PutMapping("/{id}")
    public AdminServicePackageResponse update(@PathVariable Integer id, @RequestBody AdminServicePackageRequest request) {
        return servicePackageService.updateForAdmin(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        servicePackageService.deleteForAdmin(id);
    }
}
