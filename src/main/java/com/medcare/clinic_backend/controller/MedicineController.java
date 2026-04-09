package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Medicine;
import com.medcare.clinic_backend.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    @Autowired
    private MedicineService medicineService;

    @GetMapping
    public List<Medicine> getAll() {
        return medicineService.getAllMedicines();
    }

    @PostMapping
    public Medicine create(@RequestBody Medicine medicine) {
        return medicineService.createMedicine(medicine);
    }
}