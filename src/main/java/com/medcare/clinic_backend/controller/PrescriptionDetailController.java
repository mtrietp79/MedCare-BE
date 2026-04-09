package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.PrescriptionDetail;
import com.medcare.clinic_backend.service.PrescriptionDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/prescription-details")
public class PrescriptionDetailController {

    @Autowired
    private PrescriptionDetailService prescriptionDetailService;

    // API lấy danh sách thuốc theo ID của Hồ sơ bệnh án
    @GetMapping("/record/{recordId}")
    public List<PrescriptionDetail> getByRecordId(@PathVariable Integer recordId) {
        return prescriptionDetailService.getPrescriptionByRecordId(recordId);
    }

    // API thêm thuốc vào hồ sơ
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public PrescriptionDetail create(@RequestBody PrescriptionDetail detail) {
        return prescriptionDetailService.addMedicineToPrescription(detail);
    }
}