package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.DoctorResponse;
import com.medcare.clinic_backend.dto.doctor.UpdateDoctorActiveStatusRequest;
import com.medcare.clinic_backend.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/doctors")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminDoctorController {

    @Autowired
    private DoctorService doctorService;

    @PatchMapping("/{id}/active-status")
    public DoctorResponse updateActiveStatus(
            @PathVariable Integer id,
            @RequestBody UpdateDoctorActiveStatusRequest request
    ) {
        Boolean active = resolveActiveValue(request);
        return doctorService.toDoctorResponse(doctorService.updateDoctorActiveStatus(id, active));
    }

    private Boolean resolveActiveValue(UpdateDoctorActiveStatusRequest request) {
        if (request == null) {
            return null;
        }
        if (request.getActive() != null) {
            return request.getActive();
        }
        String status = request.getStatus();
        if (status == null) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if ("ACTIVE".equals(normalized) || "HOAT_DONG".equals(normalized) || "HOAT DONG".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("INACTIVE".equals(normalized)
                || "KHONG_HOAT_DONG".equals(normalized)
                || "KHONG HOAT DONG".equals(normalized)
                || "TAM_NGUNG".equals(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }
}
