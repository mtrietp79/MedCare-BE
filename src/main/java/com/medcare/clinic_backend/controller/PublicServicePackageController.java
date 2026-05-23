package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.servicepackage.PublicServicePackageDetailResponse;
import com.medcare.clinic_backend.dto.servicepackage.PublicServicePackageResponse;
import com.medcare.clinic_backend.service.ServicePackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/service-packages")
public class PublicServicePackageController {

    @Autowired
    private ServicePackageService servicePackageService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public List<PublicServicePackageResponse> getPublicPackages() {
        return servicePackageService.getActivePublicPackages();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public PublicServicePackageDetailResponse getPublicPackageDetail(@PathVariable Integer id) {
        return servicePackageService.getPublicPackageDetail(id);
    }
}
