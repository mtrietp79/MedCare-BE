package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.service.MedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    @Autowired
    private MedicalRecordService medicalRecordService;

    @GetMapping
    public List<MedicalRecord> getAll() {
        return medicalRecordService.getAllRecords();
    }

    @GetMapping("/{id}")
    public MedicalRecord getById(@PathVariable Integer id) {
        return medicalRecordService.getRecordById(id);
    }

    // API ĐẶC BIỆT: Gọi API này kèm ID bệnh nhân để lấy toàn bộ lịch sử khám
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_PATIENT')")
    public List<MedicalRecord> getHistoryByPatient(@PathVariable Integer patientId) {
        return medicalRecordService.getHistoryByPatientId(patientId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public MedicalRecord create(@RequestBody MedicalRecord record) {
        return medicalRecordService.createRecord(record);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public MedicalRecord update(@PathVariable Integer id, @RequestBody MedicalRecord record) {
        return medicalRecordService.updateRecord(id, record);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        medicalRecordService.deleteRecord(id);
    }
}