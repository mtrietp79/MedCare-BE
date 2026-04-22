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

    @GetMapping("/{id}")
    public Medicine getById(@PathVariable Integer id) {
        return medicineService.getMedicineById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Medicine create(@RequestBody Medicine medicine) {
        return medicineService.createMedicine(medicine);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Medicine update(@PathVariable Integer id, @RequestBody Medicine medicine) {
        return medicineService.updateMedicine(id, medicine);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(@PathVariable Integer id) {
        medicineService.deleteMedicine(id);
    }
}