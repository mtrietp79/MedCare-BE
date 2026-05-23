package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.servicepackage.ServicePackageBookingRequest;
import com.medcare.clinic_backend.dto.servicepackage.ServicePackageBookingResponse;
import com.medcare.clinic_backend.dto.servicepackage.ServicePackageBookingDetailResponse;
import com.medcare.clinic_backend.dto.servicepackage.ServicePackageBookingListItemResponse;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.service.ServicePackageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patient/service-package-bookings")
@PreAuthorize("hasAuthority('ROLE_PATIENT')")
public class PatientServicePackageBookingController {

    @Autowired
    private ServicePackageService servicePackageService;

    @PostMapping
    public ServicePackageBookingResponse book(
            @RequestBody ServicePackageBookingRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return servicePackageService.bookPackage(authentication.getName(), request, httpServletRequest);
    }

    @GetMapping
    public List<ServicePackageBookingListItemResponse> getMyBookings(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return servicePackageService.getPatientBookings(authentication.getName());
    }

    @GetMapping("/{id}")
    public ServicePackageBookingDetailResponse getMyBookingDetail(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return servicePackageService.getPatientBookingDetail(authentication.getName(), id);
    }
}
