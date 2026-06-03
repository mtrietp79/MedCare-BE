package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.AdminMedicineSummaryResponse;
import com.medcare.clinic_backend.dto.MedicineRequest;
import com.medcare.clinic_backend.dto.MedicineResponse;
import com.medcare.clinic_backend.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    @Autowired
    private MedicineService medicineService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public List<MedicineResponse> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category
    ) {
        return medicineService.getAllMedicineResponses(keyword, categoryId, status, category);
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public List<String> getCategories() {
        return medicineService.getMedicineCategories();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public AdminMedicineSummaryResponse getSummary() {
        return medicineService.getAdminMedicineSummary();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public MedicineResponse getById(@PathVariable Integer id) {
        return medicineService.getMedicineResponseById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MedicineResponse create(@RequestBody MedicineRequest request) {
        return medicineService.toMedicineResponse(medicineService.createMedicine(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MedicineResponse update(@PathVariable Integer id, @RequestBody MedicineRequest request) {
        return medicineService.toMedicineResponse(medicineService.updateMedicine(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(@PathVariable Integer id) {
        medicineService.deleteMedicine(id);
    }
}
