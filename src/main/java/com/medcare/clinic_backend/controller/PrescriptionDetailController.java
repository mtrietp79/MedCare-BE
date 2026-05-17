package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.PrescriptionDetail;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.service.DoctorService;
import com.medcare.clinic_backend.service.PrescriptionDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescription-details")
public class PrescriptionDetailController {

    @Autowired
    private PrescriptionDetailService prescriptionDetailService;

    @Autowired
    private DoctorService doctorService;

    @GetMapping("/record/{recordId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public List<PrescriptionDetail> getByRecordId(@PathVariable Integer recordId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            return prescriptionDetailService.getPrescriptionByRecordIdForDoctor(recordId, currentDoctor.getId());
        }
        return prescriptionDetailService.getPrescriptionByRecordId(recordId);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public PrescriptionDetail create(@RequestBody PrescriptionDetail detail) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            return prescriptionDetailService.addMedicineToPrescriptionForDoctor(detail, currentDoctor.getId());
        }
        return prescriptionDetailService.addMedicineToPrescription(detail);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private Doctor getCurrentDoctorOrThrow(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return doctorService.getDoctorByAccountUsername(authentication.getName());
    }
}
