package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.MedicalService;
import com.medcare.clinic_backend.service.MedicalServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/medical-services")
public class MedicalServiceController {
    @Autowired
    private MedicalServiceService service;

    @GetMapping
    public List<MedicalService> getAll() {
        return service.getAll();
    }

    @PostMapping
    public MedicalService create(@RequestBody MedicalService medicalService) {
        return service.create(medicalService);
    }
}