package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.servicepackage.AdminServicePackageBookingResponse;
import com.medcare.clinic_backend.dto.servicepackage.UpdateServicePackageBookingStatusRequest;
import com.medcare.clinic_backend.service.ServicePackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/service-package-bookings", "/api/admin/service-packages/bookings"})
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminServicePackageBookingController {

    @Autowired
    private ServicePackageService servicePackageService;

    @GetMapping
    public List<AdminServicePackageBookingResponse> getAllBookings(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return servicePackageService.getAllBookingsForAdmin(status, keyword);
    }

    @PatchMapping("/{id}/status")
    public AdminServicePackageBookingResponse updateStatus(
            @PathVariable Integer id,
            @RequestBody UpdateServicePackageBookingStatusRequest request
    ) {
        return servicePackageService.updateBookingStatusForAdmin(
                id,
                request == null ? null : request.getStatus()
        );
    }
}
