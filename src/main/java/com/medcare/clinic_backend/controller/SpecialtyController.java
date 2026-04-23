package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.service.SpecialtyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/specialties")
public class SpecialtyController {

    @Autowired
    private SpecialtyService specialtyService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_PATIENT')")
    public List<Specialty> getAll() {
        return specialtyService.getAllSpecialties();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_PATIENT')")
    public Specialty getById(@PathVariable Integer id) {
        return specialtyService.getSpecialtyById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Specialty create(@RequestBody Specialty specialty) {
        return specialtyService.createSpecialty(specialty);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Specialty update(@PathVariable Integer id, @RequestBody Specialty specialty) {
        return specialtyService.updateSpecialty(id, specialty);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(@PathVariable Integer id) {
        specialtyService.deleteSpecialty(id);
    }
}
